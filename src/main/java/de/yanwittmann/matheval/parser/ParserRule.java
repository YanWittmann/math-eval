package de.yanwittmann.matheval.parser;

import de.yanwittmann.matheval.Functions;

import java.util.List;
import java.util.function.Function;

import static de.yanwittmann.matheval.lexer.Lexer.Token;

@FunctionalInterface
public interface ParserRule {
    boolean match(List<Object> tokens);

    static void replace(List<Object> tokenTree, Object replacement, int startIndex, int endIndex) {
        if (endIndex + 1 > startIndex) {
            tokenTree.subList(startIndex, endIndex + 1).clear();
        }
        tokenTree.add(startIndex, replacement);

        for (int i = 0; i < tokenTree.size(); i++) {
            System.out.println(tokenTree.get(i));
        }
        System.out.println();
    }

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
}
