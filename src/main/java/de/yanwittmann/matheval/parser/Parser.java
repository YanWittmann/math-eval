package de.yanwittmann.matheval.parser;

import de.yanwittmann.matheval.exceptions.ParsingException;
import de.yanwittmann.matheval.interpreter.MenterDebugger;
import de.yanwittmann.matheval.lexer.Lexer.TokenType;
import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.operator.Operator;
import de.yanwittmann.matheval.operator.Operators;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class Parser {

    private static final Logger LOG = LogManager.getLogger(Parser.class);

    // lru cache for parsing rules using an operator instance as key
    private final static Map<Operators, List<ParserRule>> CACHED_PARSE_RULES = new LinkedHashMap<Operators, List<ParserRule>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Operators, List<ParserRule>> eldest) {
            return size() > 5;
        }
    };
    private final static Map<Operators, List<ParserRule>> CACHED_PARSE_ONCE_RULES = new LinkedHashMap<Operators, List<ParserRule>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Operators, List<ParserRule>> eldest) {
            return size() > 5;
        }
    };

    private final Operators operators;
    private final List<ParserRule> rules = new ArrayList<>();
    private final List<ParserRule> applyOnceRules = new ArrayList<>();

    public Parser(Operators operators) {
        this.operators = operators;
        generateRules(operators);
    }

    public ParserNode parse(List<Token> tokens) {
        generateRules(operators);

        final List<Object> tokenTree = new ArrayList<>(tokens);
        for (ParserRule rule : applyOnceRules) {
            while (rule.match(tokenTree)) ;
        }

        while (true) {
            if (rules.stream().noneMatch(rule -> rule.match(tokenTree))) {
                break;
            } else if (!isType(tokenTree.get(tokenTree.size() - 1), TokenType.EOF)) {
                tokenTree.add(new Token("", TokenType.EOF));
            }
        }

        if (MenterDebugger.logParsedTokens) {
            LOG.info("Parsed tokens:\n" + toString(tokenTree));
        }

        // validate token tree
        for (Object token : tokenTree) {
            if (!isType(token, ParserNode.NodeType.STATEMENT) && !isType(token, ParserNode.NodeType.EXPORT_STATEMENT) &&
                !isType(token, ParserNode.NodeType.IMPORT_STATEMENT) && !isType(token, ParserNode.NodeType.IMPORT_INLINE_STATEMENT) &&
                !isType(token, ParserNode.NodeType.IMPORT_AS_STATEMENT)) {
                throw new ParsingException("Invalid token tree, failed on\n" + token + "\n\nin\n" + toString(tokenTree));
            }
        }

        final ParserNode root = new ParserNode(ParserNode.NodeType.ROOT, null);
        root.addChildren(tokenTree);

        return root;
    }

    public String toString(List<Object> tokens) {
        return tokens.stream().map(String::valueOf).collect(Collectors.joining("\n"));
    }

    public static boolean isLiteral(Object token) {
        return isType(token, TokenType.STRING_LITERAL) || isType(token, TokenType.NUMBER_LITERAL) ||
               isType(token, TokenType.BOOLEAN_LITERAL) || isType(token, TokenType.REGEX_LITERAL) ||
               isType(token, TokenType.OTHER_LITERAL);
    }

    public static boolean isIdentifier(Object token) {
        return isType(token, TokenType.IDENTIFIER) || isType(token, ParserNode.NodeType.IDENTIFIER_ACCESSED);
    }

    public static boolean isAssignable(Object token) {
        return isIdentifier(token) || isType(token, ParserNode.NodeType.LISTED_ELEMENTS);
    }

    public static boolean isEvaluableToValue(Object token) {
        return isType(token, TokenType.IDENTIFIER) || isType(token, ParserNode.NodeType.IDENTIFIER_ACCESSED) ||
               isType(token, ParserNode.NodeType.EXPRESSION) || isType(token, ParserNode.NodeType.FUNCTION_CALL) ||
               isLiteral(token) || isType(token, ParserNode.NodeType.PARENTHESIS_PAIR) ||
               isType(token, ParserNode.NodeType.ARRAY) || isType(token, ParserNode.NodeType.MAP) ||
               isType(token, ParserNode.NodeType.CONDITIONAL) || isType(token, ParserNode.NodeType.FUNCTION_INLINE) ||
               isType(token, ParserNode.NodeType.LOOP_FOR);
    }

    public static boolean isListable(Object token) {
        return isEvaluableToValue(token) || isType(token, ParserNode.NodeType.LISTED_ELEMENTS) ||
               isType(token, ParserNode.NodeType.MAP_ELEMENT) || isType(token, ParserNode.NodeType.FUNCTION_INLINE);
    }

    public static boolean isListFinisher(Object token) {
        return isOperator(token, "=") || isType(token, TokenType.CLOSE_PARENTHESIS) ||
               isType(token, TokenType.CLOSE_SQUARE_BRACKET) || isType(token, TokenType.CLOSE_CURLY_BRACKET);
    }

    public static boolean isFinishedStatement(Object token) {
        return isEvaluableToValue(token) || isType(token, ParserNode.NodeType.ASSIGNMENT) ||
               isType(token, ParserNode.NodeType.FUNCTION_CALL) || isType(token, ParserNode.NodeType.CURLY_BRACKET_PAIR) ||
               isType(token, ParserNode.NodeType.FUNCTION_DECLARATION) || isType(token, ParserNode.NodeType.CONDITIONAL);
    }

    public static boolean isOperator(Object token, String symbol) {
        return isType(token, TokenType.OPERATOR) && ((Token) token).getValue().equals(symbol);
    }

    public static boolean isKeyword(Object token, String keyword) {
        return isType(token, TokenType.KEYWORD) && ((Token) token).getValue().equals(keyword);
    }

    public static boolean isStatementFinisher(Object token) {
        return isType(token, TokenType.SEMICOLON) || isType(token, TokenType.NEWLINE) ||
               isType(token, TokenType.EOF) || isType(token, TokenType.CLOSE_CURLY_BRACKET) ||
               isType(token, ParserNode.NodeType.STATEMENT);
    }

    public static boolean isImportStatement(Object token) {
        return isType(token, ParserNode.NodeType.IMPORT_STATEMENT) || isType(token, ParserNode.NodeType.IMPORT_AS_STATEMENT) ||
               isType(token, ParserNode.NodeType.IMPORT_INLINE_STATEMENT);
    }

    public static boolean isType(Object token, Object type) {
        if (token == null) return false;

        if (token instanceof Token) {
            final Token cast = (Token) token;

            if (cast.getType() == type) {
                return true;
            }
        }

        if (token instanceof ParserNode) {
            final ParserNode cast = (ParserNode) token;

            if (cast.getType() == type) {
                return true;
            }
        }

        return false;
    }

    private static boolean createParenthesisRule(List<Object> tokens, Object openParenthesis, Object closeParenthesis, ParserNode.NodeType replaceNode, Object[] tokenBlacklist, Object[] tokenWhitelist, Object[] doNotInclude) {
        final ParserNode node = new ParserNode(replaceNode, null);
        int start = -1;
        int end = -1;

        for (int i = 0; i < tokens.size(); i++) {
            final Object currentToken = tokens.get(i);

            final boolean isDisallowed = Arrays.stream(tokenBlacklist).anyMatch(disallowedToken -> isType(currentToken, disallowedToken));
            final boolean isAllowed = Arrays.stream(tokenWhitelist).anyMatch(allowedToken -> isType(currentToken, allowedToken));

            if (isType(currentToken, openParenthesis)) {
                start = i;
                node.getChildren().clear();
            } else if (isDisallowed) {
                start = -1;
                node.getChildren().clear();
            } else if (start != -1) {
                if (isType(currentToken, ParserNode.NodeType.LISTED_ELEMENTS)) {
                    node.addChildren(((ParserNode) currentToken).getChildren());
                } else if (isEvaluableToValue(currentToken) || isAllowed) {
                    node.addChild(currentToken);
                } else if (isType(currentToken, closeParenthesis)) {
                    end = i;
                    break;
                } else {
                    start = -1;
                    node.getChildren().clear();
                }
            }
        }

        if (start != -1 && end != -1) {
            node.getChildren().removeIf(child -> Arrays.stream(doNotInclude).anyMatch(doNotIncludeToken -> isType(child, doNotIncludeToken)));
            ParserRule.replace(tokens, node, start, end);
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private void generateRules(Operators operators) {

        if (operators == null) {
            throw new IllegalArgumentException("Operators cannot be null");
        }

        // check if rules have already been generated for these operators
        this.rules.clear();
        this.applyOnceRules.clear();
        if (CACHED_PARSE_RULES.containsKey(operators) && CACHED_PARSE_ONCE_RULES.containsKey(operators)) {
            this.rules.addAll(CACHED_PARSE_RULES.get(operators));
            this.applyOnceRules.addAll(CACHED_PARSE_ONCE_RULES.get(operators));
            return;
        }

        // remove comments
        this.applyOnceRules.add(createRemoveTokensRule(new Object[]{TokenType.COMMENT}));
        // remove double newlines
        this.applyOnceRules.add(createRemoveDoubleTokensRule(new Object[]{TokenType.NEWLINE, TokenType.SEMICOLON}));

        // transform else if to elif
        this.applyOnceRules.add(tokens -> {
            for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);
                final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

                if (isKeyword(token, "else") && isKeyword(nextToken, "if")) {
                    tokens.set(i, new Token("elif", TokenType.KEYWORD));
                    tokens.remove(i + 1);
                    return true;
                }
            }
            return false;
        });

        // remove newline in front of elif and else
        this.applyOnceRules.add(tokens -> {
            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);
                final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

                if (isType(currentToken, TokenType.NEWLINE) && (isKeyword(nextToken, "elif") || isKeyword(nextToken, "else"))) {
                    tokens.remove(i);
                    return true;
                }
            }

            return false;
        });

        // flatten successive identifier accesses
        rules.add(tokens -> {
            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);
                if (isType(currentToken, ParserNode.NodeType.IDENTIFIER_ACCESSED)) {
                    final ParserNode currentNode = (ParserNode) currentToken;

                    for (Object child : currentNode.getChildren()) {
                        if (isType(child, ParserNode.NodeType.IDENTIFIER_ACCESSED)) {
                            final ParserNode childNode = (ParserNode) child;
                            currentNode.getChildren().addAll(currentNode.getChildren().indexOf(child), childNode.getChildren());
                            currentNode.getChildren().remove(child);
                            return true;
                        }
                    }
                }
            }
            return false;
        });

        // check for curly bracket pairs with only map elements inside to transform into a map
        rules.add(tokens -> {
            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);

                if (isType(currentToken, ParserNode.NodeType.CURLY_BRACKET_PAIR)) {
                    final ParserNode node = (ParserNode) currentToken;

                    if (node.getChildren().stream().allMatch(token -> isType(token, ParserNode.NodeType.MAP_ELEMENT))) {
                        final ParserNode mapNode = new ParserNode(ParserNode.NodeType.MAP, null);
                        mapNode.addChildren(node.getChildren());
                        tokens.set(i, mapNode);
                        return true;
                    }
                }
            }

            return false;
        });
        // check for curly bracket pairs with only statements (except for the last one optionally) to transform into a CODE_BLOCK
        rules.add(tokens -> {
            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);

                if (isType(currentToken, ParserNode.NodeType.CURLY_BRACKET_PAIR)) {
                    final ParserNode node = (ParserNode) currentToken;

                    if (node.getChildren().stream().allMatch(token -> isType(token, ParserNode.NodeType.STATEMENT) || isType(token, ParserNode.NodeType.RETURN_STATEMENT) ||
                                                                      isFinishedStatement(token))) {
                        final ParserNode blockNode = makeProperCodeBlock(node);
                        tokens.set(i, blockNode);
                        return true;
                    }
                }
            }

            return false;
        });

        rules.add(tokens -> {
            for (final Object currentToken : tokens) {
                if (isType(currentToken, ParserNode.NodeType.FUNCTION_DECLARATION)) {
                    final ParserNode node = (ParserNode) currentToken;
                    final Object functionInline = node.getChildren().get(1);

                    if (isType(functionInline, ParserNode.NodeType.FUNCTION_INLINE)) {
                        final ParserNode functionInlineNode = (ParserNode) functionInline;
                        final Object parenthesisPair = functionInlineNode.getChildren().get(0);
                        final Object codeBlock = functionInlineNode.getChildren().get(1);

                        if (isType(parenthesisPair, ParserNode.NodeType.PARENTHESIS_PAIR) && (isType(codeBlock, ParserNode.NodeType.CODE_BLOCK) || isType(codeBlock, ParserNode.NodeType.RETURN_STATEMENT))) {
                            final ParserNode parenthesisPairNode = (ParserNode) parenthesisPair;
                            final ParserNode codeBlockNode = (ParserNode) codeBlock;

                            node.getChildren().set(1, parenthesisPairNode);
                            node.addChild(codeBlockNode);
                            return true;
                        }
                    }
                }
            }

            return false;
        });

        // detect import statement
        rules.add(tokens -> {
            int state = 0;
            int start = -1;
            int end = -1;
            ParserNode.NodeType type = null;

            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);
                // import <identifier>
                // import <identifier> as <identifier>
                // import <identifier> inline

                if (state == 0 && isKeyword(currentToken, "import")) {
                    state = 1;
                    start = i;
                    type = ParserNode.NodeType.IMPORT_STATEMENT;
                } else if (state == 1 && isType(currentToken, TokenType.IDENTIFIER)) {
                    state = 2;
                } else if (state == 2 && isKeyword(currentToken, "inline")) {
                    state = 6;
                    type = ParserNode.NodeType.IMPORT_INLINE_STATEMENT;
                } else if (state == 2 && isKeyword(currentToken, "as")) {
                    state = 4;
                } else if (state == 4 && isType(currentToken, TokenType.IDENTIFIER)) {
                    state = 5;
                    type = ParserNode.NodeType.IMPORT_AS_STATEMENT;
                } else if ((state == 2 || state == 5 || state == 6) && isStatementFinisher(currentToken)) {
                    state = 3;
                    end = i;
                    break;
                } else {
                    state = 0;
                    start = -1;
                    type = null;
                }
            }

            if (state == 3) {
                final ParserNode node = new ParserNode(type, null);
                for (int i = start; i < end; i++) {
                    if (isType(tokens.get(i), TokenType.IDENTIFIER)) {
                        node.addChild(tokens.get(i));
                    }
                }
                ParserRule.replace(tokens, node, start, end);
                return true;
            }

            return false;
        });

        // detect export statement
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.EXPORT_STATEMENT, (t) -> null, 0, (t, i) -> !isType(t, TokenType.KEYWORD) && !isStatementFinisher(t), (t, i) -> true, (t, i) -> t,
                t -> isKeyword(t, "export"),
                t -> isType(t, ParserNode.NodeType.ARRAY),
                t -> isKeyword(t, "as"),
                t -> isType(t, TokenType.IDENTIFIER),
                Parser::isStatementFinisher
        ));

        // function calls
        rules.add(ParserRule.inOrderWithoutPrefixRule(ParserNode.NodeType.FUNCTION_CALL,
                (pp, p) -> false, // isType(p, TokenType.DOT)
                (t, i) -> true,

                t -> isIdentifier(t) || isEvaluableToValue(t) || (isType(t, ParserNode.NodeType.SQUARE_BRACKET_PAIR) && ((ParserNode) t).getChildren().size() == 1),
                t -> isType(t, ParserNode.NodeType.PARENTHESIS_PAIR)
        ));

        // rewrite the accessor rules from above, but better this time.
        rules.add(tokens -> {
            int state = 0;
            int start = -1;
            int end = -1;
            boolean thisChainIsInvalid = false;

            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);
                final Object previousToken = i > 0 ? tokens.get(i - 1) : null;

                final boolean isSquareBracketPair = isType(currentToken, ParserNode.NodeType.SQUARE_BRACKET_PAIR);
                final boolean isArrayAccess = isSquareBracketPair && ((ParserNode) currentToken).getChildren().size() == 1;
                final boolean isValidAccessorValue = isType(currentToken, ParserNode.NodeType.FUNCTION_CALL) || isIdentifier(currentToken) ||
                                                     isArrayAccess;
                final boolean isValidInitialValue = isEvaluableToValue(currentToken);
                final boolean isInvalidFollowUpValue = isType(currentToken, ParserNode.NodeType.PARENTHESIS_PAIR) || isType(currentToken, ParserNode.NodeType.ARRAY) ||
                                                       isType(currentToken, TokenType.DOT) || isType(currentToken, TokenType.OPEN_PARENTHESIS) ||
                                                       isType(currentToken, TokenType.OPEN_SQUARE_BRACKET);// || isType(currentToken, TokenType.COMMA);
                final boolean isInvalidPreviousValue = isKeyword(previousToken, "if");
                final boolean isValidSeparator = isType(currentToken, TokenType.DOT);

                // states:
                // 0: start, no identifier found yet
                //    isValidAccessorValue || isValidInitialValue --> state 1
                //    isValidSeparator || isSquareBracketPair --> thisChainIsInvalid = true
                // 1: initial identifier value found
                //    isValidSeparator --> state 2
                // 2: found a separator
                //    isValidAccessorValue --> state 3
                //    isInvalidFollowUpValue --> state 0
                // 3: found a valid accessor value
                //    isValidSeparator --> state 2
                //    isInvalidFollowUpValue --> state 0
                //    else --> state 4
                // 4: done

                if (state == 0 && (isValidSeparator || isSquareBracketPair)) {
                    thisChainIsInvalid = true;
                } else if (isInvalidPreviousValue) {
                    state = 0;
                    start = -1;
                } else if (state == 0 && (isValidAccessorValue || isValidInitialValue)) {
                    state = 1;
                    start = i;
                } else if (state == 1 && isValidSeparator) {
                    state = 2;
                } else if (state == 1 && isArrayAccess) {
                    state = 3;
                } else if (state == 1 && isValidAccessorValue) { // this is a function call on a square brackets pair
                    state = 3;
                } else if (state == 2 && isValidAccessorValue) {
                    state = 3;
                } else if (state == 2 && isInvalidFollowUpValue) {
                    state = 0;
                    start = -1;
                } else if (state == 3 && isValidSeparator) {
                    state = 2;
                } else if (state == 3 && isInvalidFollowUpValue) {
                    state = 0;
                } else if (state == 3) { //  && !isValidSeparator  is already implied
                    if (thisChainIsInvalid) {
                        state = 0;
                        start = -1;
                    } else {
                        state = 4;
                        end = i;
                        break;
                    }
                } else {
                    state = 0;
                    start = -1;
                }
            }


            if (state == 4) {
                final ParserNode node = new ParserNode(ParserNode.NodeType.IDENTIFIER_ACCESSED, null);
                for (int i = start; i < end; i++) {
                    final Object token = tokens.get(i);

                    if (isType(token, ParserNode.NodeType.FUNCTION_CALL)) {
                        // extract the identifier from the function call to append to the identifier accessed node
                        final ParserNode function = (ParserNode) token;

                        for (int j = function.getChildren().size() - 1; j >= 0; j--) {
                            final Object childToken = function.getChildren().get(j);
                            if (!isType(childToken, ParserNode.NodeType.PARENTHESIS_PAIR) && !isType(childToken, TokenType.KEYWORD)) {
                                function.getChildren().remove(j);

                                if (isType(childToken, ParserNode.NodeType.SQUARE_BRACKET_PAIR) && ((ParserNode) childToken).getChildren().size() == 1) {
                                    node.addChild(((ParserNode) childToken).getChildren().get(0));
                                } else {
                                    node.addChild(childToken);
                                }
                            }
                        }
                        node.addChild(function);
                    } else if (isIdentifier(token) || isEvaluableToValue(token)) {
                        node.addChild(token);
                    } else if (isType(token, ParserNode.NodeType.SQUARE_BRACKET_PAIR)) {
                        final Object child = ((ParserNode) token).getChildren().get(0);
                        if (child instanceof ParserNode) {
                            node.addChild(makeProperCodeBlock((ParserNode) child));
                        } else if (child instanceof Token) {
                            node.addChild(makeProperCodeBlock((Token) child));
                        } else {
                            throw new ParsingException("Invalid child token type on accessed identifier: " + child.getClass().getName());
                        }
                    }
                }
                ParserRule.replace(tokens, node, start, end - 1);
                return true;
            }

            return false;
        });

        rules.add(tokens -> {
            int state = 0;

            for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);

                if (state == 0 && isType(token, ParserNode.NodeType.SQUARE_BRACKET_PAIR)) {
                    final ParserNode node = new ParserNode(ParserNode.NodeType.ARRAY, null, token instanceof ParserNode ? ((ParserNode) token).getChildren() : Collections.singletonList(token));
                    ParserRule.replace(tokens, node, i, i);
                    return true;
                } else if (isIdentifier(token) || isEvaluableToValue(token) || isType(token, ParserNode.NodeType.FUNCTION_CALL) || isType(token, TokenType.CLOSE_PARENTHESIS)) {
                    state = -1;
                } else {
                    state = 0;
                }
            }

            return false;
        });

        // apply operators with both sides being evaluable to values
        for (Operator operator : operators.getOperatorsDoubleAssociative()) {
            if (operator.shouldCreateParserRule()) {
                rules.add(operator.makeParserRule());
            }
        }

        // rule for operator ->
        final Operator inlineOperator = operators.findOperator("->", true, true);
        rules.add(tokens -> {
            int state = 0;
            int start = -1;
            int end = -1;

            for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);
                final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

                if (state == 0 && isEvaluableToValue(token)) {
                    state = 1;
                    start = i;
                } else if (state == 1 && isOperator(token, inlineOperator.getSymbol())) {
                    state = 2;
                } else if (state == 2 &&
                           (
                                   isType(token, ParserNode.NodeType.CODE_BLOCK) || isEvaluableToValue(token) ||
                                   isType(token, ParserNode.NodeType.RETURN_STATEMENT) || isFinishedStatement(token)
                           ) &&
                           !isType(nextToken, TokenType.OPEN_PARENTHESIS) && !isType(nextToken, TokenType.DOT) &&
                           !isType(nextToken, TokenType.OPEN_SQUARE_BRACKET) && !isType(nextToken, TokenType.OPEN_CURLY_BRACKET) &&
                           !isType(nextToken, TokenType.OPERATOR)) {
                    state = 3;
                    end = i;
                    break;
                } else {
                    state = 0;
                    start = -1;
                }
            }

            if (state == 3) {
                final ParserNode node = new ParserNode(ParserNode.NodeType.FUNCTION_INLINE, inlineOperator);

                for (int i = start; i <= end; i++) {
                    final Object token = tokens.get(i);

                    if (i == start) {
                        if (isType(token, TokenType.IDENTIFIER)) {
                            final ParserNode parenthesis = new ParserNode(ParserNode.NodeType.PARENTHESIS_PAIR, null);
                            parenthesis.addChild(token);
                            node.addChild(parenthesis);
                        } else {
                            node.addChild(token);
                        }

                    } else if (i == end && token instanceof ParserNode) {
                        node.addChild(makeProperCodeBlock((ParserNode) token));

                    } else if (i == end && token instanceof Token) {
                        node.addChild(new ParserNode(ParserNode.NodeType.CODE_BLOCK, null, Collections.singletonList(token)));

                    } else if (!isOperator(token, inlineOperator.getSymbol())) {
                        node.addChild(token);
                    }
                }

                ParserRule.replace(tokens, node, start, end);
                return true;
            }

            return false;
        });

        // map elements
        rules.add(tokens -> {
            int state = 0;
            int key = -1;
            int value = -1;

            for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);
                final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

                if (state == 0 && (isIdentifier(token) || isLiteral(token))) {
                    state = 1;
                    key = i;
                } else if (state == 1 && isOperator(token, ":")) {
                    state = 2;
                } else if (state == 2 && isEvaluableToValue(token) && (isType(nextToken, TokenType.COMMA) || isType(nextToken, TokenType.CLOSE_CURLY_BRACKET))) {
                    state = 3;
                    value = i;
                    break;
                } else {
                    state = 0;
                    key = -1;
                }
            }

            if (state == 3) {
                final ParserNode node = new ParserNode(ParserNode.NodeType.MAP_ELEMENT, null);
                node.addChild(tokens.get(key));
                node.addChild(tokens.get(value));
                ParserRule.replace(tokens, node, key, value);
                return true;
            }

            return false;
        });

        // rule for parenthesis pairs
        rules.add(tokens -> Parser.createParenthesisRule(tokens, TokenType.OPEN_CURLY_BRACKET, TokenType.CLOSE_CURLY_BRACKET, ParserNode.NodeType.CURLY_BRACKET_PAIR,
                new Object[]{TokenType.OPEN_PARENTHESIS, TokenType.OPEN_SQUARE_BRACKET, TokenType.OPEN_CURLY_BRACKET},
                new Object[]{ParserNode.NodeType.MAP_ELEMENT, ParserNode.NodeType.STATEMENT, ParserNode.NodeType.RETURN_STATEMENT, TokenType.NEWLINE},
                new Object[]{TokenType.NEWLINE}
        ));
        rules.add(tokens -> Parser.createParenthesisRule(tokens, TokenType.OPEN_SQUARE_BRACKET, TokenType.CLOSE_SQUARE_BRACKET, ParserNode.NodeType.SQUARE_BRACKET_PAIR,
                new Object[]{TokenType.OPEN_PARENTHESIS, TokenType.OPEN_SQUARE_BRACKET, TokenType.OPEN_CURLY_BRACKET},
                new Object[]{},
                new Object[]{}
        ));
        rules.add(tokens -> Parser.createParenthesisRule(tokens, TokenType.OPEN_PARENTHESIS, TokenType.CLOSE_PARENTHESIS, ParserNode.NodeType.PARENTHESIS_PAIR,
                new Object[]{TokenType.OPEN_PARENTHESIS, TokenType.OPEN_SQUARE_BRACKET, TokenType.OPEN_CURLY_BRACKET},
                new Object[]{},
                new Object[]{}
        ));

        // listed elements , separated
        rules.add(tokens -> {
            final ParserNode node = new ParserNode(ParserNode.NodeType.LISTED_ELEMENTS, null);
            int start = -1;
            int end = -1;
            boolean includesNotListElements = false;
            boolean requiresComma = false;

            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);
                final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
                final Object beforeToken = i - 1 >= 0 ? tokens.get(i - 1) : null;

                if (isType(beforeToken, TokenType.DOT)) { // may not be an unfinished accessor
                    start = -1;
                    includesNotListElements = false;
                    node.getChildren().clear();

                } else if (!requiresComma && isListable(currentToken)) {
                    if (start == -1) start = i;
                    if (isType(currentToken, ParserNode.NodeType.LISTED_ELEMENTS)) {
                        node.addChildren(((ParserNode) currentToken).getChildren());
                    } else {
                        includesNotListElements = true;
                        node.addChild(currentToken);
                    }
                    requiresComma = true;

                } else if (isType(currentToken, TokenType.COMMA)) {
                    if (node.getChildren().size() > 0) {
                        if (!isEvaluableToValue(nextToken) && !isType(nextToken, ParserNode.NodeType.LISTED_ELEMENTS) &&
                            !isType(nextToken, ParserNode.NodeType.PARENTHESIS_PAIR) && !isType(nextToken, ParserNode.NodeType.MAP_ELEMENT)) {
                            start = -1;
                            includesNotListElements = false;
                            node.getChildren().clear();
                        }
                    }
                    requiresComma = false;

                } else if (Parser.isType(currentToken, TokenType.OPEN_PARENTHESIS) || Parser.isType(currentToken, TokenType.OPEN_SQUARE_BRACKET) ||
                           Parser.isType(currentToken, TokenType.OPEN_CURLY_BRACKET)) {
                    start = -1;
                    includesNotListElements = false;
                    node.getChildren().clear();
                    requiresComma = false;

                } else if (start != -1 && includesNotListElements && node.getChildren().size() > 1 && isListFinisher(currentToken)) {
                    end = i - 1;
                    break;

                } else if (start != -1) {
                    start = -1;
                    includesNotListElements = false;
                    node.getChildren().clear();
                    requiresComma = false;
                }
            }

            if (start != -1 && includesNotListElements && node.getChildren().size() > 1) {
                ParserRule.replace(tokens, node, start, end);
                return true;
            }

            return false;
        });

        for (Operator operator : operators.getOperatorsSingleAssociative()) {
            if (operator.shouldCreateParserRule()) {
                rules.add(operator.makeParserRule());
            }
        }

        final Operator assignment = operators.findOperator("=", true, true);
        // assignment
        rules.add(tokens -> {
            int start = -1;
            int end = -1;

            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);
                final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
                final Object afterNextToken = i + 2 < tokens.size() ? tokens.get(i + 2) : null;

                if (isAssignable(currentToken)) {
                    if (start == -1) start = i;
                } else if (isOperator(currentToken, "=")) {
                    if (start != -1) {
                        if (isEvaluableToValue(nextToken)) {
                            if (isOperator(afterNextToken, "->")) {
                                start = -1;
                            } else {
                                end = i + 1;
                                break;
                            }
                        } else {
                            start = -1;
                        }
                    }
                } else if (start != -1) {
                    start = -1;
                }
            }

            if (start != -1 && end != -1) {
                final ParserNode node = new ParserNode(ParserNode.NodeType.ASSIGNMENT, assignment);
                node.addChild(tokens.get(start));
                node.addChild(tokens.get(end));
                ParserRule.replace(tokens, node, start, end);
                return true;
            }

            return false;
        });

        // conditions via CONDITIONAL_BRANCH wrapped in CONDITIONAL
        // if (condition) { ... } elif (condition) { ... } else { ... }
        // if CONDITIONAL_BRANCH: condition, body
        // elif CONDITIONAL_BRANCH: condition, body
        // else CONDITIONAL_BRANCH: body
        rules.add(tokens -> {
            final ParserNode node = new ParserNode(ParserNode.NodeType.CONDITIONAL, null);
            int start = -1;
            int end = -1;

            for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);
                final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
                final Object nextNextToken = i + 2 < tokens.size() ? tokens.get(i + 2) : null;

                final ParserNode branch = new ParserNode(ParserNode.NodeType.CONDITIONAL_BRANCH, null);
                boolean isElse = false;
                boolean conditionStarterFound = false;

                if (isKeyword(token, "if")) {
                    start = i;
                    node.getChildren().clear();
                    conditionStarterFound = true;
                } else if (isKeyword(token, "elif")) {
                    if (start == -1) {
                        continue;
                    }
                } else if (isKeyword(token, "else")) {
                    if (start == -1) {
                        continue;
                    }
                    isElse = true;
                } else if (start != -1 && node.getChildren().size() > 0) {
                    ParserRule.replace(tokens, node, start, end);
                    return true;
                }

                if (node.getChildren().size() > 0 || conditionStarterFound) {
                    final Object conditionToken = isElse ? null : nextToken;
                    final Object bodyToken = isElse ? nextToken : nextNextToken;

                    if (!isElse) {
                        if (isType(conditionToken, ParserNode.NodeType.PARENTHESIS_PAIR)) {
                            branch.addChild(conditionToken);
                        } else {
                            start = -1;
                            node.getChildren().clear();
                            continue;
                        }
                    }

                    if (isEvaluableToValue(bodyToken) || isType(bodyToken, ParserNode.NodeType.CODE_BLOCK) ||
                        isType(bodyToken, ParserNode.NodeType.RETURN_STATEMENT) || isType(bodyToken, ParserNode.NodeType.STATEMENT)) {
                        if (bodyToken instanceof ParserNode) {
                            branch.addChild(makeProperCodeBlock((ParserNode) bodyToken));
                        } else {
                            branch.addChild(bodyToken);
                        }
                        end = isElse ? i + 1 : i + 2;
                        i = end;
                    } else {
                        start = -1;
                        node.getChildren().clear();
                        continue;
                    }

                    node.addChild(branch);

                    if (isElse) {
                        ParserRule.replace(tokens, node, start, end);
                        return true;
                    }
                }
            }

            return false;
        });

        rules.add(tokens -> {
            int state = 0;
            int start = -1;
            int end = -1;

            for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);

                if (state == 0 && isKeyword(token, "for")) {
                    state = 1;
                    start = i;
                } else if (state == 1 && isType(token, TokenType.OPEN_PARENTHESIS)) {
                    state = 2;
                } else if (state == 2 && (isIdentifier(token) || isType(token, ParserNode.NodeType.PARENTHESIS_PAIR) || isType(token, ParserNode.NodeType.ARRAY) || isType(token, ParserNode.NodeType.SQUARE_BRACKET_PAIR))) {
                    state = 3;
                } else if (state == 3 && (isOperator(token, ":") || isKeyword(token, "in"))) {
                    // state = 3; // the : is not optional, but does not change the state
                } else if (state == 3 && isEvaluableToValue(token)) {
                    state = 4;
                } else if (state == 4 && isType(token, TokenType.CLOSE_PARENTHESIS)) {
                    state = 5;
                } else if (state == 5 && (isEvaluableToValue(token) || isType(token, ParserNode.NodeType.CODE_BLOCK) ||
                                          isType(token, ParserNode.NodeType.STATEMENT))) {
                    state = 6;
                    end = i;
                    break;
                } else {
                    state = 0;
                }
            }

            if (state == 6) {
                final ParserNode node = new ParserNode(ParserNode.NodeType.LOOP_FOR, null);

                for (int i = start; i <= end; i++) {
                    final Object token = tokens.get(i);

                    if (isType(token, ParserNode.NodeType.PARENTHESIS_PAIR) || isType(token, ParserNode.NodeType.ARRAY) ||
                        isType(token, ParserNode.NodeType.SQUARE_BRACKET_PAIR) || isIdentifier(token) ||
                        isEvaluableToValue(token) || isType(token, ParserNode.NodeType.CODE_BLOCK) ||
                        isType(token, ParserNode.NodeType.STATEMENT)) {

                        node.addChild(token);
                    }
                }

                ParserRule.replace(tokens, node, start, end);
                return true;
            }

            return false;
        });

        // function declaration via assignment of a code block
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.FUNCTION_DECLARATION, (t) -> null, 1, (t, i) -> !isOperator(t, "="), (t, i) -> true,
                (t, i) -> {
                    if (i == 0) {
                        return isType(t, ParserNode.NodeType.FUNCTION_CALL) ? ((ParserNode) t).getChildren() : t;
                    } else if (i == 2 && t instanceof ParserNode) {
                        return makeProperCodeBlock((ParserNode) t);
                    } else {
                        return t;
                    }
                },
                t -> isType(t, ParserNode.NodeType.FUNCTION_CALL),
                t -> isOperator(t, "="),
                token -> isEvaluableToValue(token) || isType(token, ParserNode.NodeType.CODE_BLOCK) || isType(token, ParserNode.NodeType.RETURN_STATEMENT)
        ));
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.FUNCTION_DECLARATION, (t) -> null, 1, (t, i) -> !isOperator(t, "="), (t, i) -> true,
                (t, i) -> {
                    if (i == 0) {
                        return isType(t, ParserNode.NodeType.FUNCTION_CALL) ? ((ParserNode) t).getChildren() : t;
                    } else if (i == 1 && t instanceof ParserNode) {
                        return makeProperCodeBlock((ParserNode) t);
                    } else {
                        return t;
                    }
                },
                t -> isType(t, ParserNode.NodeType.FUNCTION_CALL),
                token -> isType(token, ParserNode.NodeType.CODE_BLOCK) || isType(token, ParserNode.NodeType.RETURN_STATEMENT)
        ));
        // function declaration via inline function
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.FUNCTION_DECLARATION, (t) -> null, 1, (t, i) -> !isOperator(t, "="), (t, i) -> true, (t, i) -> t,
                Parser::isIdentifier,
                t -> isOperator(t, "="),
                t -> isType(t, ParserNode.NodeType.FUNCTION_INLINE)
        ));
        // native functions
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.FUNCTION_DECLARATION, (t) -> null, 1, (t, i) -> true, (t, i) -> true,
                (t, i) -> {
                    if (i == 1) {
                        return isType(t, ParserNode.NodeType.FUNCTION_CALL) ? ((ParserNode) t).getChildren() : t;
                    } else {
                        return t;
                    }
                },
                t -> isKeyword(t, "native"),
                t -> isType(t, ParserNode.NodeType.FUNCTION_CALL)
        ));

        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.RETURN_STATEMENT, (t) -> null, 0, (t, i) -> !isStatementFinisher(t) && !isKeyword(t, "return"), (t, i) -> !isType(t, TokenType.CLOSE_CURLY_BRACKET), (t, i) -> t,
                t -> isKeyword(t, "return"),
                Parser::isEvaluableToValue,
                Parser::isStatementFinisher
        ));

        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.STATEMENT, (t) -> null, 0, (t, i) -> !isStatementFinisher(t), (t, i) -> !isType(t, TokenType.CLOSE_CURLY_BRACKET) && !isType(t, ParserNode.NodeType.STATEMENT), (t, i) -> t,
                Parser::isFinishedStatement,
                Parser::isStatementFinisher
        ));

        // remove all remaining unnecessary tokens like newlines
        rules.add(createRemoveTokensRule(new Object[]{TokenType.NEWLINE, TokenType.SEMICOLON, TokenType.EOF}));

        CACHED_PARSE_RULES.put(operators, new ArrayList<>(this.rules));
        CACHED_PARSE_ONCE_RULES.put(operators, new ArrayList<>(this.applyOnceRules));
    }

    private static ParserRule createRemoveTokensRule(Object[] removeTokens) {
        return tokens -> {
            for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);
                if (Arrays.stream(removeTokens).anyMatch(t -> isType(token, t))) {
                    tokens.remove(i);
                    i--;
                }
            }
            return false;
        };
    }

    private static ParserRule createRemoveDoubleTokensRule(Object[] removeTokens) {
        return tokens -> {
            for (int i = 0; i < tokens.size() - 1; i++) {
                final Object token = tokens.get(i);
                final Object nextToken = tokens.get(i + 1);
                if (Arrays.stream(removeTokens).anyMatch(t -> isType(token, t)) && Arrays.stream(removeTokens).anyMatch(t -> isType(nextToken, t))) {
                    tokens.remove(i);
                    i--;
                }
            }
            return false;
        };
    }

    private static ParserNode makeProperCodeBlock(ParserNode node) {
        if (isType(node, ParserNode.NodeType.CODE_BLOCK)) {
            return node;
        }

        final ParserNode blockNode = new ParserNode(ParserNode.NodeType.CODE_BLOCK, null);

        if (isType(node, ParserNode.NodeType.CURLY_BRACKET_PAIR)) {
            for (int j = 0; j < node.getChildren().size(); j++) {
                final Object childToken = node.getChildren().get(j);
                if (isType(childToken, ParserNode.NodeType.STATEMENT)) {
                    blockNode.addChildren(((ParserNode) childToken).getChildren());
                } else {
                    blockNode.addChild(childToken);
                }
            }
        } else {
            if (isType(node, ParserNode.NodeType.STATEMENT)) {
                blockNode.addChildren(node.getChildren());
            } else {
                blockNode.addChild(node);
            }
        }

        return blockNode;
    }

    private static ParserNode makeProperCodeBlock(Token node) {
        final ParserNode blockNode = new ParserNode(ParserNode.NodeType.CODE_BLOCK, null);
        blockNode.addChild(node);
        return blockNode;
    }
}
