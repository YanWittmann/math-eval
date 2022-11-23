package de.yanwittmann.matheval.parser;

import de.yanwittmann.matheval.lexer.Lexer;

import java.util.List;
import java.util.function.Function;

/*
static ParserRule inOrderRule(ParserNode.NodeType type, Function<Object, Object> replaceValue, int replaceValueObjectIndex, Functions.Function2<Object, Integer, Boolean> keepValue, Function<Object, Boolean>... expected) {
    return tokens -> {
        int currentMatchLength = 0;
        for (int i = 0; i < tokens.size(); i++) {
            final Object currentToken = tokens.get(i);

            if (currentToken instanceof Token || currentToken instanceof ParserNode) {
                if (currentMatchLength < expected.length && expected[currentMatchLength].apply(currentToken)) {
                    currentMatchLength++;
                } else {
                    i -= currentMatchLength > 1 ? currentMatchLength - 1 : 0;
                    currentMatchLength = 0;
                }
            }

            if (currentMatchLength == expected.length) {
                final Object replaceValueObject = replaceValueObjectIndex < 0 ? currentToken : tokens.get(i - replaceValueObjectIndex);

                final ParserNode node = new ParserNode(type, replaceValue.apply(replaceValueObject));
                for (int j = 0; j < currentMatchLength; j++) {
                    final Object token = tokens.get(i - currentMatchLength + j + 1);
                    if (keepValue.apply(token, j)) {
                        node.addChild(token);
                    }
                }

                ParserRule.replace(tokens, node, i - currentMatchLength + 1, i);
                return true;
            }
        }

        return false;
    };
}
 */
public class ParserRulePart {

    private final Function<Object, Boolean> expected;
    private final int minRepetitions;
    private final int maxRepetitions;
    private final boolean includeInResult;

    public ParserRulePart(int minRepetitions, int maxRepetitions, boolean includeInResult, Function<Object, Boolean> expected) {
        this.minRepetitions = minRepetitions;
        this.maxRepetitions = maxRepetitions;
        this.includeInResult = includeInResult;
        this.expected = expected;
    }

    public static ParserRule createRule(ParserNode.NodeType resultType, List<ParserRulePart> parts) {
        return tokens -> {
            ParserRulePart currentPart = parts.get(0);
            int currentMatchLength = 0;
            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);

                if (currentToken instanceof Lexer.Token || currentToken instanceof ParserNode) {
                    if (currentMatchLength < currentPart.maxRepetitions && currentPart.expected.apply(currentToken)) {
                        currentMatchLength++;
                    } else {
                        i -= currentMatchLength > 1 ? currentMatchLength - 1 : 0;
                        currentMatchLength = 0;
                    }
                }

                if (currentMatchLength == currentPart.maxRepetitions) {
                    if(currentPart == parts.get(parts.size() - 1)) {
                        final ParserNode node = new ParserNode(resultType, null);
                        for (int j = 0; j < currentMatchLength; j++) {
                            final Object token = tokens.get(i - currentMatchLength + j + 1);
                            if (currentPart.includeInResult) {
                                node.addChild(token);
                            }
                        }

                        ParserRule.replace(tokens, node, i - currentMatchLength + 1, i);
                        return true;
                    } else {
                        currentPart = parts.get(parts.indexOf(currentPart) + 1);
                        currentMatchLength = 0;
                    }
                }
            }

            return false;
        };
    }
}
