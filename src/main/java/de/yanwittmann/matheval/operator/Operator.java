package de.yanwittmann.matheval.operator;

import de.yanwittmann.matheval.interpreter.Value;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.parser.Parser;
import de.yanwittmann.matheval.parser.ParserNode;
import de.yanwittmann.matheval.parser.ParserRule;

import java.util.function.Function;

public interface Operator {

    String getSymbol();

    int getPrecedence();

    boolean isLeftAssociative();

    boolean isRightAssociative();

    default boolean shouldCreateParserRule() {
        return true;
    }

    ;

    Value<?> evaluate(Value<?>... arguments);

    static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    default ParserRule makeParserRule() {
        return (tokens) -> {
            for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);

                if (token instanceof Lexer.Token) {
                    final Lexer.Token operator = (Lexer.Token) token;
                    final boolean symbolEqual = getSymbol().equals(operator.getValue());

                    if (operator.getType() == Lexer.TokenType.OPERATOR && symbolEqual) {

                        final Object nextAfterToken = i + 2 < tokens.size() ? tokens.get(i + 2) : null;
                        if (Parser.isType(nextAfterToken, Lexer.TokenType.OPEN_PARENTHESIS) || Parser.isType(nextAfterToken, Lexer.TokenType.OPEN_SQUARE_BRACKET) ||
                            Parser.isType(nextAfterToken, Lexer.TokenType.OPEN_CURLY_BRACKET)) {
                            i++;
                            continue;
                        }

                        final Object before = i > 0 ? tokens.get(i - 1) : null;
                        final Object after = i < tokens.size() - 1 ? tokens.get(i + 1) : null;

                        final boolean hasBefore = Parser.isEvaluableToValue(before);
                        final boolean hasAfter = Parser.isEvaluableToValue(after);

                        final boolean leftMatches = (isLeftAssociative() && hasBefore) || (!isLeftAssociative() && !hasBefore);
                        final boolean rightMatches = (isRightAssociative() && hasAfter) || (!isRightAssociative() && !hasAfter);

                        if (leftMatches && rightMatches) {
                            final ParserNode operatorParentNode = new ParserNode(ParserNode.NodeType.EXPRESSION, this);
                            if (isLeftAssociative()) operatorParentNode.addChild(before);
                            if (isRightAssociative()) operatorParentNode.addChild(after);

                            final int leftIndex = isLeftAssociative() ? i - 1 : i;
                            final int rightIndex = isRightAssociative() ? i + 1 : i;
                            ParserRule.replace(tokens, operatorParentNode, leftIndex, rightIndex);
                            return true;
                        }
                    }
                }
            }
            return false;
        };
    }

    static Operator make(String symbol, int precedence, boolean left, boolean right, Function<Value<?>[], Value<?>> evaluator) {
        return Operator.make(symbol, precedence, left, right, evaluator, true);
    }

    static Operator make(String symbol, int precedence, boolean left, boolean right, Function<Value<?>[], Value<?>> evaluator, boolean shouldCreateParserRule) {
        return new Operator() {
            @Override
            public String getSymbol() {
                return symbol;
            }

            @Override
            public int getPrecedence() {
                return precedence;
            }

            @Override
            public boolean isLeftAssociative() {
                return left;
            }

            @Override
            public boolean isRightAssociative() {
                return right;
            }

            @Override
            public boolean shouldCreateParserRule() {
                return shouldCreateParserRule;
            }

            @Override
            public Value<?> evaluate(Value<?>... arguments) {
                return evaluator.apply(arguments);
            }

            @Override
            public String toString() {
                return symbol + " (" + precedence + (left ? " l" : "") + (right ? " r" : "") + ")";
            }
        };
    }
}
