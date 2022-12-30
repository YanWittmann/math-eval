package de.yanwittmann.matheval.operator;

import de.yanwittmann.matheval.interpreter.structure.Value;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.parser.Parser;
import de.yanwittmann.matheval.parser.ParserNode;
import de.yanwittmann.matheval.parser.ParserRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class Operator {

    public abstract String getSymbol();

    public abstract int getPrecedence();

    public abstract boolean isLeftAssociative();

    public abstract boolean isRightAssociative();

    public boolean shouldCreateParserRule() {
        return true;
    }

    public abstract Value evaluate(Value... arguments);

    public int getNumberOfArguments() {
        if (isLeftAssociative() && isRightAssociative()) {
            return 2;
        } else if (isLeftAssociative() || isRightAssociative()) {
            return 1;
        } else {
            return 0;
        }
    }

    public static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    private static class OperatorMatch {
        final ParserNode operatorParentNode;
        final int start;
        final int end;

        OperatorMatch(ParserNode operatorParentNode, int start, int end) {
            this.operatorParentNode = operatorParentNode;
            this.start = start;
            this.end = end;
        }
    }

    public ParserRule makeParserRule(List<Operator> operatorsWithPrecedence) {
        return (tokens) -> {
            final List<OperatorMatch> operatorMatches = new ArrayList<>();

            operatorMatches.add(findEarliestMatch(tokens, this));

            for (Operator operator : operatorsWithPrecedence) {
                if (operator == this) continue;
                OperatorMatch operatorMatch = findEarliestMatch(tokens, operator);
                if (operatorMatch != null) operatorMatches.add(operatorMatch);
            }

            final OperatorMatch earliestMatch = operatorMatches.stream()
                    .filter(Objects::nonNull)
                    .min(Comparator.comparingInt(match -> match.start))
                    .orElse(null);

            if (earliestMatch == null) return false;
            ParserRule.replace(tokens, earliestMatch.operatorParentNode, earliestMatch.start, earliestMatch.end);
            return true;
        };
    }

    private static OperatorMatch findEarliestMatch(List<Object> tokens, Operator checkForOperator) {
        for (int i = 0; i < tokens.size(); i++) {
            final Object token = tokens.get(i);

            if (token instanceof Token) {
                final Token operator = (Token) token;
                final boolean symbolEqual = checkForOperator.getSymbol().equals(operator.getValue());

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

                    final boolean leftMatches = (checkForOperator.isLeftAssociative() && hasBefore) || (!checkForOperator.isLeftAssociative() && !hasBefore);
                    final boolean rightMatches = (checkForOperator.isRightAssociative() && hasAfter) || (!checkForOperator.isRightAssociative() && !hasAfter);

                    if (!leftMatches || !rightMatches) {
                        continue;
                    }


                    final Object beforeBefore = i > 1 ? tokens.get(i - 2) : null;
                    final Object afterAfter = i < tokens.size() - 2 ? tokens.get(i + 2) : null;

                    if (Parser.isType(beforeBefore, Lexer.TokenType.DOT) || Parser.isType(afterAfter, Lexer.TokenType.DOT)) {
                        continue;
                    } else if (Parser.isType(before, Lexer.TokenType.CLOSE_PARENTHESIS) || Parser.isType(before, Lexer.TokenType.CLOSE_SQUARE_BRACKET) ||
                               Parser.isType(before, Lexer.TokenType.CLOSE_CURLY_BRACKET)) {
                        continue;
                    }


                    // must not be an unfinished statement
                    boolean foundUnallowedToken = false;
                    for (int j = i + 1; j < tokens.size(); j++) {
                        final Object next = tokens.get(j);
                        if (Parser.isType(next, Lexer.TokenType.OPEN_PARENTHESIS) || Parser.isType(next, Lexer.TokenType.OPEN_SQUARE_BRACKET) ||
                            Parser.isType(next, Lexer.TokenType.OPEN_CURLY_BRACKET)) {
                            foundUnallowedToken = true;
                            break;
                        }
                        if (Parser.isType(next, Lexer.TokenType.CLOSE_PARENTHESIS) || Parser.isType(next, Lexer.TokenType.CLOSE_SQUARE_BRACKET) ||
                            Parser.isType(next, Lexer.TokenType.CLOSE_CURLY_BRACKET) || Parser.isStatementFinisher(next)) {
                            break;
                        }
                    }
                    if (foundUnallowedToken) continue;


                    // all criteria met
                    final ParserNode operatorParentNode = new ParserNode(ParserNode.NodeType.EXPRESSION, checkForOperator);
                    if (checkForOperator.isLeftAssociative()) operatorParentNode.addChild(before);
                    if (checkForOperator.isRightAssociative()) operatorParentNode.addChild(after);

                    final int leftIndex = checkForOperator.isLeftAssociative() ? i - 1 : i;
                    final int rightIndex = checkForOperator.isRightAssociative() ? i + 1 : i;

                    return new OperatorMatch(operatorParentNode, leftIndex, rightIndex);
                }
            }
        }

        return null;
    }

    static Operator make(String symbol, int precedence, boolean left, boolean right, Function<Value[], Value> evaluator) {
        return Operator.make(symbol, precedence, left, right, evaluator, true);
    }

    static Operator make(String symbol, int precedence, boolean left, boolean right, Function<Value[], Value> evaluator, boolean shouldCreateParserRule) {
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
            public Value evaluate(Value... arguments) {
                return evaluator.apply(arguments);
            }

            @Override
            public String toString() {
                return symbol + " (" + precedence + (left ? " l" : "") + (right ? " r" : "") + ")";
            }
        };
    }
}
