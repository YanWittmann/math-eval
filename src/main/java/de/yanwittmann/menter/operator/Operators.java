package de.yanwittmann.menter.operator;

import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Operators {

    private static int BIG_DECIMAL_DIVISION_SCALE = 20;

    private final List<Operator> operators = new ArrayList<>();

    public static void setBigDecimalDivisionScale(int bigDecimalDivisionScale) {
        if (bigDecimalDivisionScale < 0) {
            throw new IllegalArgumentException("bigDecimalDivisionScale must be >= 0");
        } else if (bigDecimalDivisionScale > 100) {
            throw new IllegalArgumentException("bigDecimalDivisionScale must be <= 100");
        }
        BIG_DECIMAL_DIVISION_SCALE = bigDecimalDivisionScale;
    }

    public static int getBigDecimalDivisionScale() {
        return BIG_DECIMAL_DIVISION_SCALE;
    }

    public Operators() {
        // precedence values see https://introcs.cs.princeton.edu/java/11precedence/

        add(OperatorUtilities.makeLeft("++", 150, (argument) -> OperatorUtilities.operatorTypeHandler("++", argument,
                new OperatorUtilities.SingleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        (value) -> {
                            final Value before = new Value(value);
                            value.setValue(value.getNumericValue().add(BigDecimal.ONE));
                            return before;
                        }
                )
        )));
        add(OperatorUtilities.makeLeft("--", 150, (argument) -> OperatorUtilities.operatorTypeHandler("--", argument,
                new OperatorUtilities.SingleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        (value) -> {
                            final Value before = new Value(value);
                            value.setValue(value.getNumericValue().subtract(BigDecimal.ONE));
                            return before;
                        }
                )
        )));

        final BiFunction<Value, Value, Value> power = (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("^", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> {
                            if (right.getNumericValue().signum() != -1) {
                                return new Value(Math.pow(left.getNumericValue().doubleValue(), right.getNumericValue().doubleValue()));
                            } else {
                                // Math.pow doesn't support negative exponents
                                return new Value(BigDecimal.ONE.divide(BigDecimal.valueOf(Math.pow(left.getNumericValue().doubleValue(), right.getNumericValue().negate().doubleValue())), BIG_DECIMAL_DIVISION_SCALE, RoundingMode.HALF_UP));
                            }
                        }
                )
        );
        add(OperatorUtilities.makeDouble("^", 145, power));
        add(OperatorUtilities.makeDouble("^^", 145, power));
        add(OperatorUtilities.makeDouble("**", 145, power));

        add(OperatorUtilities.makeRight("++", 140, (argument) -> OperatorUtilities.operatorTypeHandler("++", argument,
                new OperatorUtilities.SingleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        (value) -> {
                            value.setValue(value.getNumericValue().add(BigDecimal.ONE));
                            return value;
                        }
                )
        )));
        add(OperatorUtilities.makeRight("--", 140, (argument) -> OperatorUtilities.operatorTypeHandler("--", argument,
                new OperatorUtilities.SingleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        (value) -> {
                            value.setValue(value.getNumericValue().subtract(BigDecimal.ONE));
                            return value;
                        }
                )
        )));

        add(OperatorUtilities.makeRight("-", 140, (argument) -> OperatorUtilities.operatorTypeHandler("-", argument,
                new OperatorUtilities.SingleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        (value) -> new Value(value.getNumericValue().negate())
                )
        )));
        add(OperatorUtilities.makeRight("!", 140, (argument) -> OperatorUtilities.operatorTypeHandler("!", argument,
                new OperatorUtilities.SingleOperatorTypeAction(
                        PrimitiveValueType.ANY.getType(),
                        (value) -> new Value(!value.isTrue())
                )
        )));
        add(OperatorUtilities.makeLeft("!", 140, (argument) -> OperatorUtilities.operatorTypeHandler("!", argument,
                new OperatorUtilities.SingleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        (value) -> {
                            BigDecimal result = BigDecimal.ONE;
                            for (BigDecimal i = BigDecimal.ONE; i.compareTo(value.getNumericValue()) <= 0; i = i.add(BigDecimal.ONE)) {
                                result = result.multiply(i);
                            }
                            return new Value(result);
                        }
                )
        )));
        add(OperatorUtilities.makeRight("~", 140, (argument) -> OperatorUtilities.operatorTypeHandler("~", argument,
                new OperatorUtilities.SingleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        (value) -> new Value(value.getNumericValue().negate().subtract(BigDecimal.ONE))
                )
        )));
        add(OperatorUtilities.makeRight("+", 140, (argument) -> OperatorUtilities.operatorTypeHandler("+", argument,
                new OperatorUtilities.SingleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        (value) -> new Value(value.getNumericValue().abs())
                )
        )));

        add(OperatorUtilities.makeDouble("*", 120, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("*", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> new Value(left.getNumericValue().multiply(right.getNumericValue()))
                ),
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.STRING.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> {
                            StringBuilder builder = new StringBuilder();
                            for (int i = 0; i < right.getNumericValue().intValue(); i++) {
                                builder.append(left.getValue());
                            }
                            return new Value(builder.toString());
                        }
                ),
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.STRING.getType(),
                        (left, right) -> {
                            StringBuilder builder = new StringBuilder();
                            for (int i = 0; i < left.getNumericValue().intValue(); i++) {
                                builder.append(right.getValue());
                            }
                            return new Value(builder.toString());
                        }
                )
        )));
        add(OperatorUtilities.makeDouble("/", 120, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("/", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> new Value(left.getNumericValue().divide(right.getNumericValue(), BIG_DECIMAL_DIVISION_SCALE, RoundingMode.HALF_UP))
                )
        )));
        add(OperatorUtilities.makeDouble("%", 120, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("%", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> new Value(left.getNumericValue().remainder(right.getNumericValue()))
                )
        )));
        add(OperatorUtilities.makeDouble("%%", 120, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("%%", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> {
                            BigDecimal leftValue = left.getNumericValue();
                            BigDecimal rightValue = right.getNumericValue();
                            BigDecimal remainder = leftValue.remainder(rightValue);
                            if (remainder.compareTo(BigDecimal.ZERO) < 0) {
                                remainder = remainder.add(rightValue);
                            }
                            return new Value(remainder);
                        }
                )
        )));
        add(OperatorUtilities.makeDouble("+", 110, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("+", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> new Value(left.getNumericValue().add(right.getNumericValue()))
                ),
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.STRING.getType(),
                        PrimitiveValueType.STRING.getType(),
                        (left, right) -> new Value(left.getValue() + String.valueOf(right.getValue()))
                ),
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.STRING.getType(),
                        PrimitiveValueType.ANY.getType(),
                        (left, right) -> new Value(left.getValue() + right.toDisplayString())
                ),
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.ANY.getType(),
                        PrimitiveValueType.STRING.getType(),
                        (left, right) -> new Value(left.toDisplayString() + right.getValue())
                )
        )));
        add(OperatorUtilities.makeDouble("-", 110, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("-", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> new Value(left.getNumericValue().subtract(right.getNumericValue()))
                )
        )));

        add(OperatorUtilities.makeDouble("<<", 100, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("<<", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> {
                            final BigInteger leftValue = left.getNumericValue().toBigInteger();
                            final BigInteger rightValue = right.getNumericValue().toBigInteger();
                            return new Value(new BigDecimal(leftValue.shiftLeft(rightValue.intValue())));
                        }
                )
        )));
        add(OperatorUtilities.makeDouble(">>", 100, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler(">>", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.NUMBER.getType(),
                        PrimitiveValueType.NUMBER.getType(),
                        (left, right) -> {
                            final BigInteger leftValue = left.getNumericValue().toBigInteger();
                            final BigInteger rightValue = right.getNumericValue().toBigInteger();
                            return new Value(new BigDecimal(leftValue.shiftRight(rightValue.intValue())));
                        }
                )
        )));

        add(OperatorUtilities.makeDouble("<", 90, (leftArgument, rightArgument) -> {
            final int cmp = leftArgument.compareTo(rightArgument);
            return new Value(cmp < 0);
        }));
        add(OperatorUtilities.makeDouble("<=", 90, (leftArgument, rightArgument) -> {
            final int cmp = leftArgument.compareTo(rightArgument);
            return new Value(cmp <= 0);
        }));
        add(OperatorUtilities.makeDouble(">", 90, (leftArgument, rightArgument) -> {
            final int cmp = leftArgument.compareTo(rightArgument);
            return new Value(cmp > 0);
        }));
        add(OperatorUtilities.makeDouble(">=", 90, (leftArgument, rightArgument) -> {
            final int cmp = leftArgument.compareTo(rightArgument);
            return new Value(cmp >= 0);
        }));

        add(OperatorUtilities.makeDouble("==", 80, (leftArgument, rightArgument) -> {
            if (leftArgument.equals(rightArgument)) {
                return new Value(leftArgument.getValue().equals(rightArgument.getValue()));
            } else {
                return new Value(false);
            }
        }));
        add(OperatorUtilities.makeDouble("!=", 80, (leftArgument, rightArgument) -> {
            if (leftArgument.equals(rightArgument)) {
                return new Value(!leftArgument.getValue().equals(rightArgument.getValue()));
            } else {
                return new Value(true);
            }
        }));

        add(OperatorUtilities.makeDouble("&", 70, (leftArgument, rightArgument) -> {
            return null;
        }));

        add(OperatorUtilities.makeDouble("|", 50, (leftArgument, rightArgument) -> {
            return null;
        }));

        add(OperatorUtilities.makeDouble("&&", 40, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("&&", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.ANY.getType(),
                        PrimitiveValueType.ANY.getType(),
                        (left, right) -> new Value(left.isTrue() && right.isTrue())
                )
        )));

        add(OperatorUtilities.makeDouble("||", 30, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("||", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        PrimitiveValueType.ANY.getType(),
                        PrimitiveValueType.ANY.getType(),
                        (left, right) -> new Value(left.isTrue() || right.isTrue())
                )
        )));

        add(OperatorUtilities.makeDouble(":::", 21, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler(":::", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        new String[]{PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.ANY.getType(), PrimitiveValueType.ANY.getType()},
                        new String[]{PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.ANY.getType(), PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.ANY.getType()},
                        Operators::objectConcatenationOperatorTriple
                )
        )));

        add(OperatorUtilities.makeDouble("::", 20, (leftArgument, rightArgument) -> OperatorUtilities.operatorTypeHandler("::", leftArgument, rightArgument,
                new OperatorUtilities.DoubleOperatorTypeAction(
                        new String[]{PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.ANY.getType(), PrimitiveValueType.ANY.getType()},
                        new String[]{PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.ANY.getType(), PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.ANY.getType()},
                        Operators::objectConcatenationOperatorTriple
                )
        )));

        // special rules for assignment/...
        add(OperatorUtilities.makeDouble("=", 10, (leftArgument, rightArgument) -> null, false));

        add(OperatorUtilities.makeDouble("->", 5, (leftArgument, rightArgument) -> null, false));

        add(OperatorUtilities.makeDouble("|>", 0, (leftArgument, rightArgument) -> null, true));

        add(OperatorUtilities.makeDouble(">|", 0, (leftArgument, rightArgument) -> null, true));
    }

    public void add(Operator operator) {
        operators.add(operator);
        operators.sort(
                Comparator
                        .comparing(Operator::getPrecedence)
                        .thenComparing(o -> o.getSymbol().length())
                        .thenComparing(o -> o.isLeftAssociative() ? 0 : 1)
                        .reversed()
        );
    }

    public void remove(Operator operator) {
        operators.remove(operator);
    }

    public List<Operator> getOperators() {
        return operators;
    }

    public List<Operator> getOperatorsSingleAssociative() {
        List<Operator> operators = new ArrayList<>();
        for (Operator operator : this.operators) {
            if (operator.isLeftAssociative() && !operator.isRightAssociative() ||
                !operator.isLeftAssociative() && operator.isRightAssociative()) {
                operators.add(operator);
            }
        }
        return operators;
    }

    public List<Operator> getOperatorsDoubleAssociative() {
        List<Operator> operators = new ArrayList<>();
        for (Operator operator : this.operators) {
            if (operator.isLeftAssociative() && operator.isRightAssociative()) {
                operators.add(operator);
            }
        }
        return operators;
    }

    public List<Operator> findOperators(String symbol) {
        final List<Operator> result = new ArrayList<>();

        for (Operator operator : operators) {
            if (operator.getSymbol().equals(symbol)) {
                result.add(operator);
            }
        }

        return result;
    }

    public Operator findOperator(String symbol, boolean leftAssociative, boolean rightAssociative) {
        for (Operator operator : operators) {
            if (operator.getSymbol().equals(symbol) && operator.isLeftAssociative() == leftAssociative && operator.isRightAssociative() == rightAssociative) {
                return operator;
            }
        }
        return null;
    }

    public List<Operator> findOperatorsWithPrecedence(int precedence) {
        return operators.stream().filter(o -> o.getPrecedence() == precedence).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operators cast = (Operators) o;
        return Objects.equals(operators, cast.operators);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operators);
    }

    private static Value objectConcatenationOperatorTriple(Value... elements) {
        // Merge two maps into a single one by concatenating the values of the same key and adding the new keys.
        final boolean areAllValuesLists = Arrays.stream(elements).allMatch(value -> !(value.getValue() instanceof Map) || Value.isMapAnArray(value));

        if (areAllValuesLists) {
            final List<Value> list = new ArrayList<>();

            for (Value element : elements) {
                if (element.getValue() instanceof Map) {
                    for (Map.Entry<Object, Value> entry : ((Map<Object, Value>) element.getValue()).entrySet()) {
                        list.add(entry.getValue());
                    }
                } else {
                    list.add(element);
                }
            }

            return new Value(list);
        } else {
            final Map<Object, Value> map = new LinkedHashMap<>();
            BigDecimal index = new BigDecimal(-1);

            for (Value element : elements) {
                if (element.getValue() instanceof Map) {
                    for (Map.Entry<Object, Value> entry : ((Map<Object, Value>) element.getValue()).entrySet()) {
                        map.put(entry.getKey(), entry.getValue());
                        if (entry.getKey() instanceof BigDecimal) {
                            index = ((BigDecimal) entry.getKey()).max(index);
                        }
                    }
                } else {
                    index = index.add(BigDecimal.ONE);
                    map.put(index, element);
                }
            }

            return new Value(map);
        }
    }
}
