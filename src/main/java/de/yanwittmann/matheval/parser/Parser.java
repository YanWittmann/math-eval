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

    public static boolean isAssignable(Object token) {
        return isType(token, TokenType.IDENTIFIER) || isType(token, ParserNode.NodeType.ACCESSOR_IDENTIFIER);
    }

    public static boolean isEvaluableToValue(Object token) {
        return isType(token, TokenType.IDENTIFIER) || isType(token, ParserNode.NodeType.ACCESSOR_IDENTIFIER) ||
               isType(token, ParserNode.NodeType.EXPRESSION) || isType(token, ParserNode.NodeType.FUNCTION_CALL) ||
               isLiteral(token);
    }

    public static boolean isOperator(Object token, String symbol) {
        return isType(token, TokenType.OPERATOR) && ((Token) token).getValue().equals(symbol);
    }

    public static boolean isStatementFinisher(Object token) {
        return isType(token, TokenType.SEMICOLON) || isType(token, TokenType.NEWLINE);
    }

    private static boolean isType(Object token, Object type) {
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
        for (Operator operator : this.operators.getOperators()) {
            if (operator.shouldCreateParserRule()) {
                rules.add(operator.makeParserRule());
            }
        }

        final Operator assignment = this.operators.findOperator("=", true, true);
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.ASSIGNMENT, (t) -> assignment, 1, (t, i) -> !isOperator(t, "="),
                Parser::isAssignable,
                (t) -> isOperator(t, "="),
                Parser::isEvaluableToValue
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
