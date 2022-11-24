package de.yanwittmann.matheval.parser;

import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.lexer.Lexer.TokenType;
import de.yanwittmann.matheval.operator.Operator;
import de.yanwittmann.matheval.operator.Operators;

import java.util.ArrayList;
import java.util.List;

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

    public static boolean isLiteral(Object token) {
        return isType(token, TokenType.STRING_LITERAL) || isType(token, TokenType.NUMBER_LITERAL) ||
               isType(token, TokenType.BOOLEAN_LITERAL) || isType(token, TokenType.REGEX_LITERAL) ||
               isType(token, TokenType.OTHER_LITERAL);
    }

    public static boolean isIdentifier(Object token) {
        return isType(token, TokenType.IDENTIFIER) || isType(token, ParserNode.NodeType.ACCESSOR_IDENTIFIER);
    }

    public static boolean isEvaluableToValue(Object token) {
        return isType(token, TokenType.IDENTIFIER) || isType(token, ParserNode.NodeType.ACCESSOR_IDENTIFIER) ||
               isType(token, ParserNode.NodeType.EXPRESSION) || isType(token, ParserNode.NodeType.FUNCTION_CALL) ||
               isLiteral(token) || isType(token, ParserNode.NodeType.PARENTHESIS_PAIR) ||
               isType(token, ParserNode.NodeType.ARRAY) || isType(token, ParserNode.NodeType.MAP);
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

    @SuppressWarnings("unchecked")
    private void generateRules() {

        // accessors
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.ACCESSOR_IDENTIFIER, (t) -> null, 0, (t, i) -> !isType(t, TokenType.DOT),
                Parser::isIdentifier,
                (t) -> isType(t, TokenType.DOT),
                Parser::isIdentifier
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
                final Object nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

                final boolean isComma = isType(currentToken, TokenType.COMMA);

                if (isEvaluableToValue(currentToken) || isType(currentToken, ParserNode.NodeType.PARENTHESIS_PAIR)) {
                    if (start == -1) start = i;
                    includesNotListElements = true;
                    node.addChild(currentToken);
                } else if (isType(currentToken, ParserNode.NodeType.LISTED_ELEMENTS)) {
                    if (start == -1) start = i;
                    node.addChildren(((ParserNode) currentToken).getChildren());
                } else if (isComma) {
                    if (node.getChildren().size() > 0) {
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

        // rule for parenthesis pairs ()
        rules.add(tokens -> {
            final ParserNode node = new ParserNode(ParserNode.NodeType.PARENTHESIS_PAIR, null);
            int start = -1;
            int end = -1;

            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);

                if (isType(currentToken, TokenType.OPEN_PARENTHESIS)) {
                    start = i;
                    node.getChildren().clear();
                } else if (start != -1) {
                    if (isType(currentToken, ParserNode.NodeType.LISTED_ELEMENTS)) {
                        node.addChildren(((ParserNode) currentToken).getChildren());
                    } else if (isEvaluableToValue(currentToken)) {
                        node.addChild(currentToken);
                    } else if (isType(currentToken, TokenType.CLOSE_PARENTHESIS)) {
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
        });

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
