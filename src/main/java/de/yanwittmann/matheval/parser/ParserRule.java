package de.yanwittmann.matheval.parser;

import de.yanwittmann.matheval.interpreter.MenterDebugger;
import de.yanwittmann.matheval.lexer.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@FunctionalInterface
public interface ParserRule {

    Logger LOG = LogManager.getLogger(ParserRule.class);

    boolean match(List<Object> tokens);

    static void replace(List<Object> tokenTree, Object replacement, int startIndex, int endIndex) {
        if (endIndex + 1 > startIndex) {
            tokenTree.subList(startIndex, endIndex + 1).clear();
        }
        tokenTree.add(startIndex, replacement);

        if (MenterDebugger.logParseProgress) {
            tokenTree.forEach(LOG::info);
            LOG.info(IntStream.range(0, 20).mapToObj(i -> "-").collect(Collectors.joining(" ")));
        }
    }

    static ParserRule inOrderRule(ParserNode.NodeType type, Function<Object, Object> replaceValue, int replaceValueObjectIndex, BiFunction<Object, Integer, Boolean> keepValue, BiFunction<Object, Integer, Boolean> replacePadding, BiFunction<Object, Integer, Object> replaceChildMapper, Function<Object, Boolean>... expected) {
        return tokens -> {
            int state = 0;
            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);

                if (currentToken instanceof Token || currentToken instanceof ParserNode) {
                    if (state < expected.length && expected[state].apply(currentToken)) {
                        state++;
                    } else {
                        if (state > 1) {
                            i -= state - 1;
                        } else if (state == 1) {
                            i--;
                        }
                        state = 0;
                    }
                }

                if (state == expected.length) {
                    final Object replaceValueObject = replaceValueObjectIndex < 0 ? currentToken : tokens.get(i - replaceValueObjectIndex);

                    final ParserNode node = new ParserNode(type, replaceValue.apply(replaceValueObject));
                    int paddedValueLength = 0;
                    for (int j = 0; j < state; j++) {
                        final Object token = replaceChildMapper.apply(tokens.get(i - state + j + 1), j);

                        if (!replacePadding.apply(token, j)) {
                            paddedValueLength++;
                        } else if (keepValue.apply(token, j)) {
                            if (token instanceof Collection) {
                                node.addChildren((Collection<Object>) token);
                            } else {
                                node.addChild(token);
                            }
                        }
                    }

                    ParserRule.replace(tokens, node, i - state + 1, i - paddedValueLength);
                    return true;
                }
            }

            return false;
        };
    }

    static ParserRule inOrderWithoutPrefixRule(ParserNode.NodeType type, BiFunction<Object, Object, Boolean> disallowedPrefix, BiFunction<Object, Integer, Boolean> keepValue, Function<Object, Boolean>... expected) {
        return tokens -> {
            int state = 0;
            int start = -1;
            int end = -1;

            for (int i = 0; i < tokens.size(); i++) {
                final Object currentToken = tokens.get(i);
                final Object previousToken = i > 0 ? tokens.get(i - 1) : null;

                if (currentToken instanceof Token || currentToken instanceof ParserNode) {
                    if (disallowedPrefix.apply(previousToken, currentToken)) {
                        state = -1;
                        start = -1;
                        continue;
                    } else if (state == -1) {
                        state = 0;
                        continue;
                    }

                    if (state < expected.length && expected[state].apply(currentToken)) {
                        if (state == 0) {
                            start = i;
                        }
                        state++;
                    } else {
                        if (state > 1) {
                            i -= state - 1;
                        } else if (state == 1) {
                            i--;
                        }
                        state = 0;
                    }
                }

                if (state == expected.length) {
                    end = i;
                    break;
                }
            }

            if (start != -1 && end != -1) {
                final ParserNode node = new ParserNode(type, null);
                for (int i = start; i <= end; i++) {
                    final Object token = tokens.get(i);

                    if (keepValue.apply(token, i - start)) {
                        if (token instanceof Collection) {
                            node.addChildren((Collection<Object>) token);
                        } else {
                            node.addChild(token);
                        }
                    }
                }
                ParserRule.replace(tokens, node, start, end);
                return true;
            }

            return false;
        };
    }
}
