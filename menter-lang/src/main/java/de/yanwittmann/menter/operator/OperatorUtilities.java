package de.yanwittmann.menter.operator;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OperatorUtilities {

    private interface OperatorTypeAction {
    }

    public static class DoubleOperatorTypeAction implements OperatorTypeAction {
        private final String[] leftTypes;
        private final String[] rightTypes;
        private final BiFunction<Value, Value, Value> action;

        public DoubleOperatorTypeAction(String[] leftTypes, String[] rightTypes, BiFunction<Value, Value, Value> action) {
            this.leftTypes = leftTypes;
            this.rightTypes = rightTypes;
            this.action = action;
        }

        public DoubleOperatorTypeAction(String leftType, String rightType, BiFunction<Value, Value, Value> action) {
            this(new String[]{leftType}, new String[]{rightType}, action);
        }

        public boolean isCallable(String leftType, String rightType) {
            for (String l : leftTypes) {
                for (String r : rightTypes) {
                    if (typeMatches(l, leftType) && typeMatches(r, rightType)) return true;
                }
            }
            return false;
        }

        public Value evaluate(Value leftValue, Value rightValue) {
            if (isCallable(leftValue.getType(), rightValue.getType())) {
                return action.apply(leftValue, rightValue);
            } else {
                throw new MenterExecutionException("Cannot call operator with types " + leftValue.getType() + " and " + rightValue.getType() + "\n" +
                                                   "Possible type combinations are: " + IntStream.range(0, leftTypes.length).mapToObj(i -> leftTypes[i] + " x " + rightTypes[i]).collect(Collectors.joining(", ")));
            }
        }
    }

    public static class SingleOperatorTypeAction implements OperatorTypeAction {
        private final String[] types;
        private final Function<Value, Value> action;

        public SingleOperatorTypeAction(String[] types, Function<Value, Value> action) {
            this.types = types;
            this.action = action;
        }

        public SingleOperatorTypeAction(String type, Function<Value, Value> action) {
            this(new String[]{type}, action);
        }

        public boolean isCallable(String type) {
            for (String t : types) {
                if (typeMatches(t, type)) return true;
            }
            return false;
        }

        public Value evaluate(Value value) {
            if (isCallable(value.getType())) {
                return action.apply(value);
            } else {
                throw new MenterExecutionException("Cannot call operator " + " with type " + value.getType() + "\nValid types: " + String.join(", ", types));
            }
        }
    }

    private static boolean typeMatches(String checkType, String variableType) {
        return checkType.equals(variableType) || checkType.equals(PrimitiveValueType.ANY.getType());
    }

    public static Value operatorTypeHandler(String symbol, Value left, Value right, DoubleOperatorTypeAction... actions) {
        for (DoubleOperatorTypeAction action : actions) {
            if (action.isCallable(left.getType(), right.getType())) {
                return action.evaluate(left, right);
            }
        }

        final boolean leftIsMap = left.getValue() instanceof Map;
        final boolean rightIsMap = right.getValue() instanceof Map;

        if (leftIsMap && rightIsMap) {
            final Map<Object, Value> leftMap = left.getMap();
            final Map<Object, Value> rightMap = right.getMap();

            if (leftMap.size() != rightMap.size()) {
                throw new MenterExecutionException("Both objects must have the same size, but was: " + leftMap.size() + " != " + rightMap.size());
            }

            if (!leftMap.keySet().stream().allMatch(rightMap::containsKey) || !rightMap.keySet().stream().allMatch(leftMap::containsKey)) {
                throw new MenterExecutionException("Both objects must contain the same keys, but was: " + leftMap.keySet() + " " + symbol + " " + rightMap.keySet());
            }

            final Map<Object, Value> newMap = new LinkedHashMap<>();
            for (Map.Entry<Object, Value> leftEntry : leftMap.entrySet()) {
                if (rightMap.containsKey(leftEntry.getKey())) {
                    newMap.put(leftEntry.getKey(), operatorTypeHandler(symbol, leftEntry.getValue(), rightMap.get(leftEntry.getKey()), actions));
                }
            }

            return new Value(newMap);

        } else if (leftIsMap) {
            final Map<Object, Value> map = left.getMap();
            final Map<Object, Value> newMap = new LinkedHashMap<>();
            for (Map.Entry<Object, Value> entry : map.entrySet()) {
                newMap.put(entry.getKey(), operatorTypeHandler(symbol, entry.getValue(), right, actions));
            }
            return new Value(newMap);

        } else if (rightIsMap) {
            final Map<Object, Value> map = right.getMap();
            final Map<Object, Value> newMap = new LinkedHashMap<>();
            for (Map.Entry<Object, Value> entry : map.entrySet()) {
                newMap.put(entry.getKey(), operatorTypeHandler(symbol, left, entry.getValue(), actions));
            }
            return new Value(newMap);
        }

        throw new MenterExecutionException("No matching type combination found for " + left + " " + symbol + " " + right + "\n" +
                                           "Available combination" + (actions.length == 1 ? "" : "s") + ": " + Arrays.stream(actions)
                                                   .map(action -> action.leftTypes[0] + " " + symbol + " " + action.rightTypes[0])
                                                   .reduce((s, s2) -> s + ", " + s2)
                                                   .orElse("none")
        );
    }

    public static Value operatorTypeHandler(String symbol, Value value, SingleOperatorTypeAction... actions) {
        for (SingleOperatorTypeAction action : actions) {
            if (action.isCallable(value.getType())) {
                return action.evaluate(value);
            }
        }

        if (value instanceof Map) {
            final Map<Object, Value> map = value.getMap();
            final Map<Object, Value> newMap = new LinkedHashMap<>();
            for (Map.Entry<Object, Value> entry : map.entrySet()) {
                newMap.put(entry.getKey(), operatorTypeHandler(symbol, entry.getValue(), actions));
            }
            return new Value(newMap);
        }

        throw new MenterExecutionException("No matching type found for " + symbol + " on " + value + "\n" +
                                           "Available types: " + Arrays.stream(actions)
                                                   .map(action -> action.types[0])
                                                   .collect(Collectors.joining(", "))
        );
    }

    public static Operator makeRight(String symbol, int precedence, Function<Value, Value> evaluator) {
        return makeSingle(symbol, precedence, evaluator, true, false);
    }

    public static Operator makeLeft(String symbol, int precedence, Function<Value, Value> evaluator) {
        return makeSingle(symbol, precedence, evaluator, true, true);
    }

    public static Operator makeLeft(String symbol, int precedence, Function<Value, Value> evaluator, boolean shouldCreateParserRule) {
        return makeSingle(symbol, precedence, evaluator, shouldCreateParserRule, true);
    }

    public static Operator makeRight(String symbol, int precedence, Function<Value, Value> evaluator, boolean shouldCreateParserRule) {
        return makeSingle(symbol, precedence, evaluator, shouldCreateParserRule, false);
    }

    public static Operator makeDouble(String symbol, int precedence, BiFunction<Value, Value, Value> evaluator) {
        return makeDouble(symbol, precedence, evaluator, true);
    }

    public static Operator makeDouble(String symbol, int precedence, BiFunction<Value, Value, Value> evaluator, boolean shouldCreateParserRule) {
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
                return true;
            }

            @Override
            public boolean isRightAssociative() {
                return true;
            }

            @Override
            public boolean shouldCreateParserRule() {
                return shouldCreateParserRule;
            }

            @Override
            public Value evaluate(Value... arguments) {
                if (arguments.length != 2) {
                    throw new MenterExecutionException(getSymbol() + " expected 2 arguments, got " + arguments.length);
                }
                return evaluator.apply(arguments[0], arguments[1]);
            }
        };
    }

    private static Operator makeSingle(String symbol, int precedence, Function<Value, Value> evaluator, boolean shouldCreateParserRule, boolean left) {
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
                return !left;
            }

            @Override
            public boolean shouldCreateParserRule() {
                return shouldCreateParserRule;
            }

            @Override
            public Value evaluate(Value... arguments) {
                if (arguments.length != 1) {
                    throw new MenterExecutionException(getSymbol() + " expected 1 argument, got " + arguments.length);
                }
                return evaluator.apply(arguments[0]);
            }
        };
    }
}
