package de.yanwittmann.matheval.parser;

import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.lexer.Lexer.TokenType;
import de.yanwittmann.matheval.operator.Operator;
import de.yanwittmann.matheval.operator.Operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static de.yanwittmann.matheval.lexer.Lexer.Token;

public class Parser {

    private final Lexer lexer;
    private final Operators operators;
    private final List<ParserRule> rules = new ArrayList<>();
    private final List<Object> tokenTree = new ArrayList<>();

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.operators = lexer.getOperators();

        generateRules();
        parse();

        System.out.println("\nParsed syntax tree:");
        for (int i = 0; i < tokenTree.size(); i++) {
            System.out.println(tokenTree.get(i));
        }
    }

    public String testGetTokenTree() {
        return tokenTree.stream().map(String::valueOf).collect(Collectors.joining("\n"));
    }

    public static boolean isLiteral(Object token) {
        return isType(token, TokenType.STRING_LITERAL) || isType(token, TokenType.NUMBER_LITERAL) ||
               isType(token, TokenType.BOOLEAN_LITERAL) || isType(token, TokenType.REGEX_LITERAL) ||
               isType(token, TokenType.OTHER_LITERAL);
    }

    public static boolean isIdentifier(Object token) {
        return isType(token, TokenType.IDENTIFIER) || isType(token, ParserNode.NodeType.IDENTIFIER_ACCESSED);
    }

    public static boolean isEvaluableToValue(Object token) {
        return isType(token, TokenType.IDENTIFIER) || isType(token, ParserNode.NodeType.IDENTIFIER_ACCESSED) ||
               isType(token, ParserNode.NodeType.EXPRESSION) || isType(token, ParserNode.NodeType.FUNCTION_CALL) ||
               isLiteral(token) || isType(token, ParserNode.NodeType.PARENTHESIS_PAIR) ||
               isType(token, ParserNode.NodeType.ARRAY) || isType(token, ParserNode.NodeType.MAP);
    }

    public static boolean isListable(Object token) {
        return isEvaluableToValue(token) || isType(token, ParserNode.NodeType.LISTED_ELEMENTS) ||
               isType(token, ParserNode.NodeType.FUNCTION);
    }

    public static boolean isFinishedStatement(Object token) {
        return isEvaluableToValue(token) || isType(token, ParserNode.NodeType.ASSIGNMENT) ||
               isType(token, ParserNode.NodeType.FUNCTION_CALL);
    }

    public static boolean isOperator(Object token, String symbol) {
        return isType(token, TokenType.OPERATOR) && ((Token) token).getValue().equals(symbol);
    }

    public static boolean isStatementFinisher(Object token) {
        return isType(token, TokenType.SEMICOLON) || isType(token, TokenType.NEWLINE) || isType(token, TokenType.EOF);
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

    private static boolean createParenthesisRule(List<Object> tokens, Object openParenthesis, Object closeParenthesis, ParserNode.NodeType replaceNode, Object... disallowedTokens) {
        final ParserNode node = new ParserNode(replaceNode, null);
        int start = -1;
        int end = -1;

        for (int i = 0; i < tokens.size(); i++) {
            final Object currentToken = tokens.get(i);

            final boolean isDisallowed = Arrays.stream(disallowedTokens).anyMatch(disallowedToken -> isType(currentToken, disallowedToken));

            if (isType(currentToken, openParenthesis)) {
                start = i;
                node.getChildren().clear();
            } else if (isDisallowed) {
                start = -1;
                node.getChildren().clear();
            } else if (start != -1) {
                if (isType(currentToken, ParserNode.NodeType.LISTED_ELEMENTS)) {
                    node.addChildren(((ParserNode) currentToken).getChildren());
                } else if (isEvaluableToValue(currentToken)) {
                    node.addChild(currentToken);
                } else if (isType(currentToken, closeParenthesis)) {
                    end = i;
                    break;
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
    private void generateRules() {

        // accessor using . for literals and functions/identifiers
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.IDENTIFIER_ACCESSED, (t) -> null, 0, (t, i) -> !isType(t, TokenType.DOT),
                Parser::isLiteral,
                (t) -> isType(t, TokenType.DOT),
                (t) -> isType(t, ParserNode.NodeType.FUNCTION_CALL) || isIdentifier(t)
        ));
        // accessor using . for identifiers
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.IDENTIFIER_ACCESSED, (t) -> null, 0, (t, i) -> !isType(t, TokenType.DOT),
                Parser::isIdentifier,
                (t) -> isType(t, TokenType.DOT),
                Parser::isIdentifier
        ));

        // accessor using []
        rules.add(ParserRulePart.createRule(ParserNode.NodeType.IDENTIFIER_ACCESSED, Arrays.asList(
                new ParserRulePart(1, 1, Parser::isIdentifier, t -> t),
                new ParserRulePart(1, 1, (t) -> isType(t, ParserNode.NodeType.SQUARE_BRACKET_PAIR), t -> ((ParserNode) t).getChildren())
        )));

        // after it has been confirmed that there are no more accessors, [] can be converted to arrays
        rules.add(ParserRulePart.createRule(ParserNode.NodeType.ARRAY, Collections.singletonList(
                new ParserRulePart(1, 1, (t) -> isType(t, ParserNode.NodeType.SQUARE_BRACKET_PAIR), t -> ((ParserNode) t).getChildren())
        )));

        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.FUNCTION_CALL, (t) -> null, 0, (t, i) -> true,
                Parser::isIdentifier,
                (t) -> isType(t, ParserNode.NodeType.PARENTHESIS_PAIR)
        ));

        // function calls
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.FUNCTION_CALL, (t) -> null, 0, (t, i) -> true,
                Parser::isIdentifier,
                (t) -> isType(t, ParserNode.NodeType.PARENTHESIS_PAIR)
        ));

        // rule for listed elements , separated
        rules.add(tokens -> {
            final ParserNode node = new ParserNode(ParserNode.NodeType.LISTED_ELEMENTS, null);
            int start = -1;
            int end = -1;
            boolean includesNotListElements = false;

            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);

                if (isListable(currentToken)) {
                    if (start == -1) start = i;
                    if (isType(currentToken, ParserNode.NodeType.LISTED_ELEMENTS)) {
                        node.addChildren(((ParserNode) currentToken).getChildren());
                    } else {
                        includesNotListElements = true;
                        node.addChild(currentToken);
                    }
                } else if (isType(currentToken, TokenType.COMMA)) {
                    if (node.getChildren().size() > 0) {
                        final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

                        if (!isEvaluableToValue(nextToken) && !isType(nextToken, ParserNode.NodeType.LISTED_ELEMENTS) && !isType(nextToken, ParserNode.NodeType.PARENTHESIS_PAIR)) {
                            start = -1;
                            includesNotListElements = false;
                            node.getChildren().clear();
                        }
                    }
                } else if (Parser.isType(currentToken, TokenType.OPEN_PARENTHESIS)) {
                    start = -1;
                    includesNotListElements = false;
                    node.getChildren().clear();
                } else if (start != -1) {
                    end = i - 1;
                    break;
                }
            }

            if (start != -1 && includesNotListElements && node.getChildren().size() > 1) {
                ParserRule.replace(tokens, node, start, end);
                return true;
            }

            return false;
        });

        for (Operator operator : this.operators.getOperators()) {
            if (operator.shouldCreateParserRule()) {
                rules.add(operator.makeParserRule());
            }
        }

        // rule for parenthesis pairs
        rules.add(tokens -> Parser.createParenthesisRule(tokens, TokenType.OPEN_SQUARE_BRACKET, TokenType.CLOSE_SQUARE_BRACKET, ParserNode.NodeType.SQUARE_BRACKET_PAIR, TokenType.OPEN_PARENTHESIS));
        rules.add(tokens -> Parser.createParenthesisRule(tokens, TokenType.OPEN_PARENTHESIS, TokenType.CLOSE_PARENTHESIS, ParserNode.NodeType.PARENTHESIS_PAIR, TokenType.OPEN_SQUARE_BRACKET));

        final Operator assignment = this.operators.findOperator("=", true, true);
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.ASSIGNMENT, (t) -> assignment, 1, (t, i) -> !isOperator(t, "="),
                Parser::isIdentifier,
                (t) -> isOperator(t, "="),
                Parser::isEvaluableToValue
        ));

        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.STATEMENT, (t) -> null, 0, (t, i) -> !isStatementFinisher(t),
                Parser::isFinishedStatement,
                Parser::isStatementFinisher
        ));
    }

    private void parse() {
        final List<Token> tokens = lexer.getTokens();
        tokenTree.addAll(tokens);

        while (true) {
            if (rules.stream().noneMatch(rule -> rule.match(tokenTree))) {
                break;
            }
        }
    }
}
