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

    public static boolean isLiteral(Token token) {
        return token.getType() == TokenType.STRING_LITERAL || token.getType() == TokenType.NUMBER_LITERAL ||
               token.getType() == TokenType.BOOLEAN_LITERAL || token.getType() == TokenType.REGEX_LITERAL ||
               token.getType() == TokenType.OTHER_LITERAL;
    }

    private boolean isTokenType(Object token, TokenType type) {
        return token instanceof Token && ((Token) token).getType() == type;
    }

    @SuppressWarnings("unchecked")
    private void generateRules() {
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.LITERAL, (t) -> null, (t) -> isTokenType(t, TokenType.NUMBER_LITERAL)));
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.LITERAL, (t) -> null, (t) -> isTokenType(t, TokenType.STRING_LITERAL)));
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.LITERAL, (t) -> null, (t) -> isTokenType(t, TokenType.BOOLEAN_LITERAL)));
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.LITERAL, (t) -> null, (t) -> isTokenType(t, TokenType.REGEX_LITERAL)));
        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.LITERAL, (t) -> null, (t) -> isTokenType(t, TokenType.OTHER_LITERAL)));

        rules.add(ParserRule.inOrderRule(ParserNode.NodeType.OPERATOR, (t) -> null, (t) -> isTokenType(t, TokenType.OPERATOR)));

        for (Operator operator : this.operators.getOperators()) {
            rules.add(operator.makeParserRule());
        }
    }

    private void parse() {
        final List<Token> tokens = lexer.getTokens();
        tokenTree.addAll(tokens);

        while (true) {
            boolean matched = false;
            for (ParserRule rule : rules) {
                if (rule.match(tokenTree)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) break;
        }
    }
}
