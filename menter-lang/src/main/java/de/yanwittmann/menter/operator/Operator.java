package de.yanwittmann.menter.operator;

import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.lexer.Lexer.TokenType;
import de.yanwittmann.menter.lexer.Token;
import de.yanwittmann.menter.parser.Parser;
import de.yanwittmann.menter.parser.ParserNode;
import de.yanwittmann.menter.parser.ParserRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public abstract class Operator {
    private static final Logger LOG = LogManager.getLogger(Operator.class);

    public abstract String getSymbol();

    public abstract int getPrecedence();

    public abstract boolean isLeftAssociative();

    public abstract boolean isRightAssociative();

    public boolean shouldCreateParserRule() {
        return true;
    }

    public abstract Value evaluate(Value... arguments);

    public Value evaluate(Collection<Value> arguments) {
        return evaluate(arguments.toArray(new Value[0]));
    }

    public int getArgumentCount() {
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

    @Override
    public String toString() {
        return getSymbol() + " (" + getPrecedence() + (isLeftAssociative() ? " l" : "") + (isRightAssociative() ? " r" : "") + ")";
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

        @Override
        public String toString() {
            return operatorParentNode.getValue().toString() + " (" + start + " - " + end + ")";
        }
    }

    public ParserRule makeParserRule(List<Operator> operatorsWithPrecedence) {
        return (tokens) -> {
            final List<OperatorMatch> operatorMatches = new ArrayList<>();

            operatorMatches.add(findEarliestMatch(tokens, this));
            if (operatorMatches.get(0) == null) return false;

            for (Operator operator : operatorsWithPrecedence) {
                if (operator == this) continue;
                final OperatorMatch operatorMatch = findEarliestMatch(tokens, operator);
                if (operatorMatch != null) operatorMatches.add(operatorMatch);
            }

            if (operatorMatches.size() > 0) {
                final OperatorMatch earliestMatch = operatorMatches.stream()
                        .min(Comparator.comparingInt(match -> match.start))
                        .get();

                ParserRule.replace(tokens, earliestMatch.operatorParentNode, earliestMatch.start, earliestMatch.end);
                return true;
            }

            return false;
        };
    }

    private static OperatorMatch findEarliestMatch(List<Object> tokens, Operator checkForOperator) {
        final boolean isPipelineOperator = checkForOperator.getSymbol().equals("|>") || checkForOperator.getSymbol().equals(">|");

        for (int i = 0; i < tokens.size(); i++) {
            final Object token = tokens.get(i);

            if (token instanceof Token) {
                final Token operatorCandidate = (Token) token;
                final boolean symbolEqual = checkForOperator.getSymbol().equals(operatorCandidate.getValue());

                if (operatorCandidate.getType() == TokenType.OPERATOR && symbolEqual) {

                    final Object nextAfterToken = i + 2 < tokens.size() ? tokens.get(i + 2) : null;
                    if (Parser.isType(nextAfterToken, TokenType.OPEN_PARENTHESIS) || Parser.isType(nextAfterToken, TokenType.OPEN_SQUARE_BRACKET) ||
                        Parser.isType(nextAfterToken, TokenType.OPEN_CURLY_BRACKET)) {
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

                    if (Parser.isType(beforeBefore, TokenType.DOT) || Parser.isType(afterAfter, TokenType.DOT)) {
                        continue;
                    } else if (isBlockCloser(before)) {
                        continue;
                    }


                    // must not be an unfinished statement
                    int depthStartPosition = i;
                    int currentDepthDiff = 0;
                    for (int j = depthStartPosition; j > 0; j--) {
                        final Object tokenBefore = tokens.get(j);

                        if (currentDepthDiff == 0 && isStatementSeparator(tokenBefore)) {
                            depthStartPosition = j + 1;
                            break;
                        }

                        if (isBlockOpener(tokenBefore)) {
                            currentDepthDiff--;
                        } else if (isBlockCloser(tokenBefore)) {
                            currentDepthDiff++;
                        }

                        if (currentDepthDiff < 0 || j == 1) {
                            depthStartPosition = j + 1;
                            break;
                        }
                    }

                    boolean foundUnallowedToken = false;
                    for (int j = depthStartPosition; j < tokens.size(); j++) {
                        final Object next = tokens.get(j);

                        if (isBlockOpener(next)) {
                            foundUnallowedToken = true;
                            break;
                        }
                        if (isBlockCloser(next) || isStatementSeparator(next)) {
                            break;
                        }

                        if (isPipelineOperator && j != i && Parser.isType(next, TokenType.OPERATOR) && !((Token) next).getValue().equals("|>") && !((Token) next).getValue().equals(">|")) {
                            foundUnallowedToken = true;
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

    private static boolean isBlockCloser(Object before) {
        return Parser.isType(before, TokenType.CLOSE_PARENTHESIS) || Parser.isType(before, TokenType.CLOSE_SQUARE_BRACKET) ||
               Parser.isType(before, TokenType.CLOSE_CURLY_BRACKET);
    }

    private static boolean isBlockOpener(Object after) {
        return Parser.isType(after, TokenType.OPEN_PARENTHESIS) || Parser.isType(after, TokenType.OPEN_SQUARE_BRACKET) ||
               Parser.isType(after, TokenType.OPEN_CURLY_BRACKET);
    }

    private static boolean isStatementSeparator(Object after) {
        return Parser.isType(after, TokenType.SEMICOLON) || Parser.isType(after, TokenType.NEWLINE) ||
               Parser.isType(after, TokenType.EOF) || Parser.isType(after, ParserNode.NodeType.ASSIGNMENT);
    }
}
