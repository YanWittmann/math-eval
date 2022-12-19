package de.yanwittmann.matheval.parser;

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
            }
        }

        if (MenterDebugger.logParsedTokens) {
            LOG.info("Parsed tokens:\n" + toString(tokenTree));
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
               isType(token, ParserNode.NodeType.CONDITIONAL);
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

    private static boolean createParenthesisRule(List<Object> tokens, Object openParenthesis, Object closeParenthesis, ParserNode.NodeType replaceNode, Object[] tokenBlacklist, Object[] tokenWhitelist) {
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

        // detect inline function declaration assignments and transform them into a regular function declaration
        // FUNCTION_DECLARATION: = (10 l r)
        // ├─ IDENTIFIER: test
        // └─ FUNCTION_INLINE: -> (0 l r)
        //    ├─ PARENTHESIS_PAIR
        //    │  └─ IDENTIFIER: x
        //    └─ CODE_BLOCK
        //       └─ EXPRESSION: + (110 l r)
        //          ├─ IDENTIFIER: x
        //          └─ NUMBER_LITERAL: 1
        // to:
        // FUNCTION_DECLARATION: = (10 l r)
        // ├─ IDENTIFIER: test
        // ├─ PARENTHESIS_PAIR
        // │  └─ IDENTIFIER: x
        // └─ CODE_BLOCK
        //    └─ RETURN_STATEMENT
        //       └─ EXPRESSION: + (110 l r)
        //          ├─ IDENTIFIER: x
        //          └─ NUMBER_LITERAL: 1
        rules.add(tokens -> {
            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);

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

        // accessor using . for literals and functions/identifiers
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.IDENTIFIER_ACCESSED, (t) -> null, 0, (t, i) -> !isType(t, TokenType.DOT), (t, i) -> true,
                (t, i) -> t, Parser::isLiteral,
                (t) -> isType(t, TokenType.DOT),
                (t) -> isType(t, ParserNode.NodeType.FUNCTION_CALL) || isIdentifier(t)
        ));
        // accessor using . for identifiers
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.IDENTIFIER_ACCESSED, (t) -> null, 0, (t, i) -> !isType(t, TokenType.DOT), (t, i) -> true, (t, i) -> t,
                Parser::isIdentifier,
                (t) -> isType(t, TokenType.DOT),
                Parser::isIdentifier
        ));

        // accessor using []
        /*rules.add(ParserRulePart.createRule(ParserNode.NodeType.IDENTIFIER_ACCESSED, Arrays.asList(
                new ParserRulePart(1, 1, Parser::isIdentifier, t -> t),
                new ParserRulePart(1, 1, (t) -> isType(t, ParserNode.NodeType.SQUARE_BRACKET_PAIR), t -> ((ParserNode) t).getChildren())
        )));*/
        // accessor using []
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.IDENTIFIER_ACCESSED, (t) -> null, 0, (t, i) -> !isType(t, ParserNode.NodeType.SQUARE_BRACKET_PAIR), (t, i) -> true,
                (t, i) -> {
                    if (isType(t, ParserNode.NodeType.SQUARE_BRACKET_PAIR)) {
                        final Object child = ((ParserNode) t).getChildren().get(0);
                        if (child instanceof ParserNode) {
                            return makeProperCodeBlock((ParserNode) child);
                        }
                        return child;
                    } else {
                        return t;
                    }
                },
                Parser::isIdentifier,
                (t) -> isType(t, ParserNode.NodeType.SQUARE_BRACKET_PAIR)
        ));


        // after it has been confirmed that there are no more accessors, [] can be converted to arrays
        rules.add(ParserRulePart.createRule(ParserNode.NodeType.ARRAY, Collections.singletonList(
                new ParserRulePart(1, 1, (t) -> isType(t, ParserNode.NodeType.SQUARE_BRACKET_PAIR), t -> ((ParserNode) t).getChildren())
        )));

        // function calls
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.FUNCTION_CALL, (t) -> null, 0, (t, i) -> true, (t, i) -> true, (t, i) -> t,
                Parser::isIdentifier,
                (t) -> isType(t, ParserNode.NodeType.PARENTHESIS_PAIR)
        ));

        for (Operator operator : operators.getOperatorsDoubleAssociative()) {
            if (operator.shouldCreateParserRule()) {
                rules.add(operator.makeParserRule());
            }
        }

        // rule for operator ->
        final Operator inlineOperator = operators.findOperator("->", true, true);
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.FUNCTION_INLINE, (t) -> inlineOperator, 0, (t, i) -> !isType(t, TokenType.OPERATOR), (t, i) -> true,
                (t, i) -> {
                    if (i == 0) {
                        if (isType(t, TokenType.IDENTIFIER)) {
                            final ParserNode node = new ParserNode(ParserNode.NodeType.PARENTHESIS_PAIR, null);
                            node.addChild(t);
                            return node;
                        } else {
                            return t;
                        }
                    } else if (i == 2 && t instanceof ParserNode) {
                        return makeProperCodeBlock((ParserNode) t);
                    } else {
                        return t;
                    }
                },
                Parser::isEvaluableToValue,
                (t) -> isOperator(t, "->"),
                (t) -> isType(t, ParserNode.NodeType.CODE_BLOCK) || isEvaluableToValue(t) || isType(t, ParserNode.NodeType.RETURN_STATEMENT)
        ));

        // detect : in a map, an identifier before it, and a value after it
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.MAP_ELEMENT, (t) -> null, 0, (t, i) -> !isOperator(t, ":"), (t, i) -> true, (t, i) -> t,
                Parser::isIdentifier,
                (t) -> isOperator(t, ":"),
                Parser::isEvaluableToValue
        ));

        // rule for parenthesis pairs
        rules.add(tokens -> Parser.createParenthesisRule(tokens, TokenType.OPEN_CURLY_BRACKET, TokenType.CLOSE_CURLY_BRACKET, ParserNode.NodeType.CURLY_BRACKET_PAIR,
                new Object[]{TokenType.OPEN_PARENTHESIS, TokenType.OPEN_SQUARE_BRACKET, TokenType.OPEN_CURLY_BRACKET}, new Object[]{ParserNode.NodeType.MAP_ELEMENT, ParserNode.NodeType.STATEMENT, ParserNode.NodeType.RETURN_STATEMENT}));
        rules.add(tokens -> Parser.createParenthesisRule(tokens, TokenType.OPEN_SQUARE_BRACKET, TokenType.CLOSE_SQUARE_BRACKET, ParserNode.NodeType.SQUARE_BRACKET_PAIR,
                new Object[]{TokenType.OPEN_PARENTHESIS, TokenType.OPEN_SQUARE_BRACKET, TokenType.OPEN_CURLY_BRACKET}, new Object[]{}));
        rules.add(tokens -> Parser.createParenthesisRule(tokens, TokenType.OPEN_PARENTHESIS, TokenType.CLOSE_PARENTHESIS, ParserNode.NodeType.PARENTHESIS_PAIR,
                new Object[]{TokenType.OPEN_PARENTHESIS, TokenType.OPEN_SQUARE_BRACKET, TokenType.OPEN_CURLY_BRACKET}, new Object[]{}));

        // rule for listed elements , separated
        rules.add(tokens -> {
            final ParserNode node = new ParserNode(ParserNode.NodeType.LISTED_ELEMENTS, null);
            int start = -1;
            int end = -1;
            boolean includesNotListElements = false;
            boolean requiresComma = false;

            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);
                final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

                if (!requiresComma && isListable(currentToken)) {
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

                    if (isEvaluableToValue(bodyToken) || isType(bodyToken, ParserNode.NodeType.CODE_BLOCK) || isType(bodyToken, ParserNode.NodeType.RETURN_STATEMENT)) {
                        branch.addChild(makeProperCodeBlock((ParserNode) bodyToken));
                        end = isElse ? i + 1 : i + 2;
                        i = end;
                    } else {
                        start = -1;
                        node.getChildren().clear();
                        continue;
                    }

                    node.addChild(branch);
                }
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
                token -> isEvaluableToValue(token) || isType(token, ParserNode.NodeType.CODE_BLOCK) || isType(token, ParserNode.NodeType.RETURN_STATEMENT)
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
}
