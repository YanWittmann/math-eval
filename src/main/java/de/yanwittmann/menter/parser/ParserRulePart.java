package de.yanwittmann.menter.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;


public class ParserRulePart {

    private final Function<Object, Boolean> expected;
    private final Function<Object, Object> transformValue;
    private final int minRepetitions;
    private final int maxRepetitions;

    public ParserRulePart(int minRepetitions, int maxRepetitions, Function<Object, Boolean> expected, Function<Object, Object> transformValue) {
        this.minRepetitions = minRepetitions;
        this.maxRepetitions = maxRepetitions;
        this.expected = expected;
        this.transformValue = transformValue;
    }

    public static ParserRule createRule(ParserNode.NodeType resultType, List<ParserRulePart> parts) {
        return tokens -> {
            ParserRulePart currentPart = parts.get(0);
            int matchStart = -1;
            int currentMatchLength = 0;
            final List<Object> targetChildNodes = new ArrayList<>();
            Object resultValue = null;

            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);

                if (currentPart.expected.apply(currentToken)) {
                    if (matchStart == -1) matchStart = i;
                    currentMatchLength++;

                    final Object transformedToken = currentPart.transformValue.apply(currentToken);
                    if (transformedToken != null) {
                        if (transformedToken instanceof Collection) {
                            targetChildNodes.addAll((Collection<?>) transformedToken);
                        } else {
                            targetChildNodes.add(transformedToken);
                        }
                    }

                    if (currentMatchLength == currentPart.maxRepetitions) {
                        if (parts.indexOf(currentPart) == parts.size() - 1) {
                            final ParserNode node = new ParserNode(resultType, resultValue);
                            node.addChildren(targetChildNodes);

                            ParserRule.replace(tokens, node, matchStart, i);
                            return true;
                        } else {
                            currentPart = parts.get(parts.indexOf(currentPart) + 1);
                            currentMatchLength = 0;
                        }
                    }
                } else {
                    if (currentMatchLength == 0 || currentMatchLength < currentPart.minRepetitions) {
                        i -= currentMatchLength;
                        currentMatchLength = 0;
                        matchStart = -1;
                        targetChildNodes.clear();
                        currentPart = parts.get(0);
                    } else {
                        currentMatchLength = 0;
                        currentPart = parts.get(parts.indexOf(currentPart) + 1);
                    }
                }
            }

            return false;
        };
    }
}
