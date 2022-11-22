package de.yanwittmann.matheval.operator;

import de.yanwittmann.matheval.interpreter.Value;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.parser.ParserNode;
import de.yanwittmann.matheval.parser.ParserRule;

import java.util.List;
import java.util.function.Function;

public interface Operator {

    String getSymbol();

    int getPrecedence();

    boolean isLeftAssociative();

    boolean isRightAssociative();

    Value<?> evaluate(Value<?>... arguments);

    default boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    default ParserRule makeParserRule() {
        return (tokens) -> {
            for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);

                if (token instanceof ParserNode) {
                    final ParserNode operator = (ParserNode) token;
                    final List<Object> operatorChildren = operator.getChildren();
                    final boolean symbolEqual = getSymbol().equals(operatorChildren.size() > 0 && operatorChildren.get(0) instanceof Lexer.Token ? ((Lexer.Token) operatorChildren.get(0)).getValue() : null);

                    if (operator.getType() == ParserNode.NodeType.OPERATOR && symbolEqual) {
                        final Object before = i > 0 ? tokens.get(i - 1) : null;
                        final Object after = i < tokens.size() - 1 ? tokens.get(i + 1) : null;

                        final boolean hasBefore = isValidOperatorArgument(before);
                        final boolean hasAfter = isValidOperatorArgument(after);

                        final boolean leftMatches = (isLeftAssociative() && hasBefore) || (!isLeftAssociative() && !hasBefore);
                        final boolean rightMatches = (isRightAssociative() && hasAfter) || (!isRightAssociative() && !hasAfter);

                        if (leftMatches && rightMatches) {
                            final ParserNode operatorParentNode = new ParserNode(ParserNode.NodeType.EXPRESSION, this);
                            if (isLeftAssociative()) operatorParentNode.addChild(before);
                            if (isRightAssociative()) operatorParentNode.addChild(after);

                            final int leftIndex = isLeftAssociative() ? i - 1 : i;
                            final int rightIndex = isRightAssociative() ? i + 1 : i;
                            ParserRule.replace(tokens, leftIndex, rightIndex, operatorParentNode);
                            return true;
                        }
                    }
                }
            }
            return false;
        };
    }

    default boolean isValidOperatorArgument(Object argument) {
        if (argument instanceof ParserNode) {
            final ParserNode parsed = (ParserNode) argument;
            final ParserNode.NodeType type = parsed.getType();

            if (type == ParserNode.NodeType.EXPRESSION) {
                return true;
            } else if (type == ParserNode.NodeType.LITERAL) {
                return true;
            }
        }

        return false;
    }

    static Operator make(String symbol, int precedence, boolean left, boolean right, Function<Value<?>[], Value<?>>
            evaluator) {
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
