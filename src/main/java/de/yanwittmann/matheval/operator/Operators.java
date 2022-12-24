package de.yanwittmann.matheval.operator;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.interpreter.structure.PrimitiveValueType;
import de.yanwittmann.matheval.interpreter.structure.Value;

import java.math.BigDecimal;
import java.util.*;

public class Operators {

    private final List<Operator> operators = new ArrayList<>();

    private static Optional<Value> getNumericValue(Value value) {
        try {
            if (value.getType().equals(PrimitiveValueType.NUMBER.getType())) {
                return Optional.of(value);
            } else if (value.getType().equals(PrimitiveValueType.STRING.getType())) {
                return Optional.of(new Value(new BigDecimal((String) value.getValue())));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<String> getStringValue(Value value) {
        if (value.getType().equals(PrimitiveValueType.STRING.getType())) {
            return Optional.of((String) value.getValue());
        } else {
            return Optional.empty();
        }
    }

    private static void throwCannotPerformOperationException(String symbol, Value... values) {
        StringBuilder builder = new StringBuilder();
        builder.append("Cannot perform operation '").append(symbol).append("' with arguments: ");
        for (int i = 0; i < values.length; i++) {
            builder.append(values[i]);
            if (i < values.length - 1) {
                builder.append(", ");
            }
        }
        throw new MenterExecutionException(builder.toString());
    }

    public Operators() {
        // precedence values see https://introcs.cs.princeton.edu/java/11precedence/

        add(Operator.make("++", 150, true, false, (arguments) -> {
            return null;
        }));
        add(Operator.make("--", 150, true, false, (arguments) -> {
            return null;
        }));

        add(Operator.make("++", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("--", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("+", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("-", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("!", 140, false, true, (arguments) -> {
            final Value value = arguments[0];
            if (value.getType().equals(PrimitiveValueType.BOOLEAN.getType())) {
                return new Value(!(boolean) value.getValue());
            } else {
                throwCannotPerformOperationException("!", value);
                return null;
            }
        }));
        add(Operator.make("!", 140, true, false, (arguments) -> {
            return null;
        }));
        add(Operator.make("~", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("~", 140, false, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("*", 120, true, true, (arguments) -> {
            final Optional<Value> left = getNumericValue(arguments[0]);
            final Optional<Value> right = getNumericValue(arguments[1]);

            if (left.isPresent() && right.isPresent()) {
                return new Value(((BigDecimal) left.get().getValue()).multiply((BigDecimal) right.get().getValue()));
            }

            final Optional<String> leftString = getStringValue(arguments[0]);
            final Optional<String> rightString = getStringValue(arguments[1]);

            if (leftString.isPresent() && rightString.isPresent()) {
                return new Value(leftString.get() + rightString.get());
            }

            throwCannotPerformOperationException("*", arguments);
            return null;
        }));
        add(Operator.make("/", 120, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("%", 120, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("+", 110, true, true, (arguments) -> {
            final Optional<Value> left = getNumericValue(arguments[0]);
            final Optional<Value> right = getNumericValue(arguments[1]);

            if (left.isPresent() && right.isPresent()) {
                return new Value(((BigDecimal) left.get().getValue()).add((BigDecimal) right.get().getValue()));
            }

            final Optional<String> leftString = getStringValue(arguments[0]);
            final Optional<String> rightString = getStringValue(arguments[1]);

            if (leftString.isPresent() && rightString.isPresent()) {
                return new Value(leftString.get() + rightString.get());
            }

            throwCannotPerformOperationException("+", arguments);
            return null;
        }));
        add(Operator.make("-", 110, true, true, (arguments) -> {
            final Optional<Value> left = getNumericValue(arguments[0]);
            final Optional<Value> right = getNumericValue(arguments[1]);

            if (left.isPresent() && right.isPresent()) {
                return new Value(((BigDecimal) left.get().getValue()).subtract((BigDecimal) right.get().getValue()));
            }

            throwCannotPerformOperationException("-", arguments);
            return null;
        }));

        add(Operator.make("<<", 100, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make(">>", 100, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("<", 90, true, true, (arguments) -> {
            final int cmp = arguments[0].compareTo(arguments[1]);
            return new Value(cmp < 0);
        }));
        add(Operator.make("<=", 90, true, true, (arguments) -> {
            final int cmp = arguments[0].compareTo(arguments[1]);
            return new Value(cmp <= 0);
        }));
        add(Operator.make(">", 90, true, true, (arguments) -> {
            final int cmp = arguments[0].compareTo(arguments[1]);
            return new Value(cmp > 0);
        }));
        add(Operator.make(">=", 90, true, true, (arguments) -> {
            final int cmp = arguments[0].compareTo(arguments[1]);
            return new Value(cmp >= 0);
        }));

        add(Operator.make("==", 80, true, true, (arguments) -> {
            final Value left = arguments[0];
            final Value right = arguments[1];
            if (left.equals(right)) {
                return new Value(left.getValue().equals(right.getValue()));
            } else {
                return new Value(false);
            }
        }));
        add(Operator.make("!=", 80, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("&", 70, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("^", 60, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("^^", 60, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("|", 50, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("&&", 40, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("||", 30, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("?", 20, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("=", 10, true, true, (arguments) -> {
            return null;
        }, false));

        add(Operator.make("->", 0, true, true, (arguments) -> {
            return null;
        }, false));
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
}
