package de.yanwittmann.matheval.parser;

import java.util.List;
import java.util.function.Function;

import static de.yanwittmann.matheval.lexer.Lexer.Token;

@FunctionalInterface
public interface ParserRule {
    boolean match(List<Object> tokens);

    static void replace(List<Object> tokenTree, int startIndex, int endIndex, Object replacement) {
        if (endIndex + 1 > startIndex) {
            tokenTree.subList(startIndex, endIndex + 1).clear();
        }
        tokenTree.add(startIndex, replacement);
    }

    static ParserRule inOrderRule(ParserNode.NodeType type, Function<Object, Object> replaceValue, Function<Object, Boolean>... expected) {
        return tokens -> {
            int currentMatchLength = 0;
            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);

                if (currentToken instanceof Token) {
                    if (currentMatchLength < expected.length && expected[currentMatchLength].apply((Token) currentToken)) {
                        currentMatchLength++;
                    } else {
                        i -= currentMatchLength > 1 ? currentMatchLength - 1 : 0;
                        currentMatchLength = 0;
                    }
                } else if (currentToken instanceof ParserNode) {
                    if (currentMatchLength < expected.length && expected[currentMatchLength].apply(((ParserNode) currentToken))) {
                        currentMatchLength++;
                    } else {
                        i -= currentMatchLength > 1 ? currentMatchLength - 1 : 0;
                        currentMatchLength = 0;
                    }
                }

                if (currentMatchLength == expected.length) {
                    final ParserNode node = new ParserNode(type, replaceValue.apply(currentToken));
                    for (int j = 0; j < currentMatchLength; j++) {
                        node.addChild(tokens.get(i - currentMatchLength + j + 1));
                    }
                    ParserRule.replace(tokens, i - currentMatchLength + 1, i, node);
                    return true;
                }
            }
            return false;
        };
    }
}
