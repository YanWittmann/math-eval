package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.operator.Operators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class CoreModuleMath {

    static {
        EvaluationContext.registerNativeFunction("math.mtr", "range", CoreModuleMath::range);
        EvaluationContext.registerNativeFunction("math.mtr", "space", CoreModuleMath::space);

        EvaluationContext.registerNativeFunction("math.mtr", "sin", CoreModuleMath::sin);
        EvaluationContext.registerNativeFunction("math.mtr", "cos", CoreModuleMath::cos);
        EvaluationContext.registerNativeFunction("math.mtr", "tan", CoreModuleMath::tan);
        EvaluationContext.registerNativeFunction("math.mtr", "asin", CoreModuleMath::asin);
        EvaluationContext.registerNativeFunction("math.mtr", "acos", CoreModuleMath::acos);
        EvaluationContext.registerNativeFunction("math.mtr", "atan", CoreModuleMath::atan);

        EvaluationContext.registerNativeFunction("math.mtr", "random", CoreModuleMath::random);
        EvaluationContext.registerNativeFunction("math.mtr", "round", CoreModuleMath::round);
        EvaluationContext.registerNativeFunction("math.mtr", "floor", CoreModuleMath::floor);
        EvaluationContext.registerNativeFunction("math.mtr", "ceil", CoreModuleMath::ceil);
        EvaluationContext.registerNativeFunction("math.mtr", "abs", CoreModuleMath::abs);

        EvaluationContext.registerNativeFunction("math.mtr", "sqrt", CoreModuleMath::sqrt);
        EvaluationContext.registerNativeFunction("math.mtr", "root", CoreModuleMath::root);
        EvaluationContext.registerNativeFunction("math.mtr", "log", CoreModuleMath::log);
        EvaluationContext.registerNativeFunction("math.mtr", "ln", CoreModuleMath::ln);
    }

    public static Value range(List<Value> arguments) {
        if (arguments.size() < 2) throw new MenterExecutionException("range() expects 2 or 3 arguments");
        final Object start = arguments.get(0).getValue();
        final Object end = arguments.get(1).getValue();
        final Object stepSizeParam = arguments.size() == 3 ? arguments.get(2).getValue() : BigDecimal.ONE;

        if (!(stepSizeParam instanceof BigDecimal)) {
            throw new MenterExecutionException("range() expects a number as step size");
        }
        final BigDecimal stepSize = (BigDecimal) stepSizeParam;
        if (stepSize.compareTo(BigDecimal.ZERO) == 0) {
            throw new MenterExecutionException("range() expects a non-zero step size");
        }

        if (start instanceof BigDecimal && end instanceof BigDecimal) {
            final BigDecimal startDecimal = (BigDecimal) start;
            final BigDecimal endDecimal = (BigDecimal) end;
            final List<Value> values = new ArrayList<>();

            if (startDecimal.compareTo(endDecimal) > 0) {
                for (BigDecimal i = startDecimal; i.compareTo(endDecimal) >= 0; i = i.subtract(stepSize)) {
                    values.add(new Value(i));
                }
            } else {
                for (BigDecimal i = startDecimal; i.compareTo(endDecimal) <= 0; i = i.add(stepSize)) {
                    values.add(new Value(i));
                }
            }
            return new Value(values);
        } else if (start instanceof String && end instanceof String) {
            final String startString = (String) start;
            final String endString = (String) end;
            final List<Value> values = new ArrayList<>();

            if (startString.length() != 1 || endString.length() != 1) {
                throw new MenterExecutionException("range() expects 2 one-character strings");
            }

            final char startChar = startString.charAt(0);
            final char endChar = endString.charAt(0);
            if (startChar > endChar) {
                for (char i = startChar; i >= endChar; i -= stepSize.intValue()) {
                    values.add(new Value(String.valueOf(i)));
                }
            } else {
                for (char i = startChar; i <= endChar; i += stepSize.intValue()) {
                    values.add(new Value(String.valueOf(i)));
                }
            }

            return new Value(values);
        } else {
            throw new MenterExecutionException("range() expects 2 numbers or 2 one-character strings");
        }
    }

    public static Value space(List<Value> parameters) {
        final BigDecimal min = (BigDecimal) parameters.get(0).getValue();
        final BigDecimal max = (BigDecimal) parameters.get(1).getValue();
        final BigDecimal range = max.subtract(min);

        // defaults to 120 values
        final int targetValueCount = parameters.size() == 3 ? ((BigDecimal) parameters.get(2).getValue()).intValue() : 120;

        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return CoreModuleMath.range(Arrays.asList(new Value(parameters.get(0)), new Value(parameters.get(1)), new Value(BigDecimal.ONE)));
        }

        // Calculate the number of steps as a BigDecimal
        final BigDecimal stepCount = new BigDecimal(targetValueCount - 1);

        // Calculate the step size using BigDecimal division with RoundingMode.HALF_UP
        final BigDecimal stepSize = range.divide(stepCount, Operators.getBigDecimalDivisionScale(), RoundingMode.HALF_UP);

        return CoreModuleMath.range(Arrays.asList(new Value(min), new Value(max), new Value(stepSize)));
    }

    private static Value applySingleValueFunction(String name, List<Value> arguments, Function<BigDecimal, BigDecimal> function) {
        if (arguments.size() != 1) throw new MenterExecutionException(name + "() expects 1 argument");
        final Object value = arguments.get(0).getValue();
        if (!(value instanceof BigDecimal)) {
            throw new MenterExecutionException(name + "() expects a number as argument");
        }
        return new Value(function.apply((BigDecimal) value));
    }

    private static Value applyDoubleValueFunction(String name, List<Value> arguments, BiFunction<BigDecimal, BigDecimal, BigDecimal> function) {
        if (arguments.size() != 2) throw new MenterExecutionException(name + "() expects 2 arguments");
        final Object a = arguments.get(0).getValue();
        final Object b = arguments.get(1).getValue();
        if (!(a instanceof BigDecimal)) {
            throw new MenterExecutionException(name + "() expects a number as first argument");
        } else if (!(b instanceof BigDecimal)) {
            throw new MenterExecutionException(name + "() expects a number as second argument");
        }
        return new Value(function.apply((BigDecimal) a, (BigDecimal) b));
    }

    public static Value sin(List<Value> arguments) {
        return applySingleValueFunction("sin", arguments, v -> BigDecimal.valueOf(Math.sin(v.doubleValue())));
    }

    public static Value cos(List<Value> arguments) {
        return applySingleValueFunction("cos", arguments, v -> BigDecimal.valueOf(Math.cos(v.doubleValue())));
    }

    public static Value tan(List<Value> arguments) {
        return applySingleValueFunction("tan", arguments, v -> BigDecimal.valueOf(Math.tan(v.doubleValue())));
    }

    public static Value asin(List<Value> arguments) {
        return applySingleValueFunction("asin", arguments, v -> BigDecimal.valueOf(Math.asin(v.doubleValue())));
    }

    public static Value acos(List<Value> arguments) {
        return applySingleValueFunction("acos", arguments, v -> BigDecimal.valueOf(Math.acos(v.doubleValue())));
    }

    public static Value atan(List<Value> arguments) {
        return applySingleValueFunction("atan", arguments, v -> BigDecimal.valueOf(Math.atan(v.doubleValue())));
    }

    public static Value random(List<Value> arguments) {
        if (arguments.size() > 2)
            throw new MenterExecutionException("random() expects 0, 1 or 2 number arguments: random(), random(max), random(min, max)");

        if (arguments.size() == 0) {
            return new Value(BigDecimal.valueOf(Math.random()));
        } else if (arguments.size() == 1) {
            final Object value = arguments.get(0).getValue();
            if (!(value instanceof BigDecimal)) {
                throw new MenterExecutionException("random() expects a number as argument");
            }
            return new Value(BigDecimal.valueOf(Math.random()).multiply((BigDecimal) value));
        } else {
            final Object value1 = arguments.get(0).getValue();
            final Object value2 = arguments.get(1).getValue();
            if (!(value1 instanceof BigDecimal) || !(value2 instanceof BigDecimal)) {
                throw new MenterExecutionException("random() expects a number as argument");
            }
            final BigDecimal min = (BigDecimal) value1;
            final BigDecimal max = (BigDecimal) value2;
            return new Value(min.add(BigDecimal.valueOf(Math.random()).multiply(max.subtract(min))));
        }
    }

    private static Value round(List<Value> values) {
        if (values.size() < 1 || values.size() > 2) {
            throw new MenterExecutionException("round() expects 1 or 2 arguments: round(number, [precision])");
        }

        final Object value = values.get(0).getValue();
        if (!(value instanceof BigDecimal)) {
            throw new MenterExecutionException("round() expects a number as first argument");
        }
        final BigDecimal number = (BigDecimal) value;

        if (values.size() == 1) {
            return new Value(number.setScale(0, RoundingMode.HALF_UP));
        } else {
            final Object precisionValue = values.get(1).getValue();
            if (!(precisionValue instanceof BigDecimal)) {
                throw new MenterExecutionException("round() expects a number as second argument");
            }
            final BigDecimal precision = (BigDecimal) precisionValue;
            return new Value(number.setScale(precision.intValue(), RoundingMode.HALF_UP));
        }
    }

    private static Value floor(List<Value> values) {
        return applySingleValueFunction("floor", values, v -> v.setScale(0, RoundingMode.FLOOR));
    }

    private static Value ceil(List<Value> values) {
        return applySingleValueFunction("ceil", values, v -> v.setScale(0, RoundingMode.CEILING));
    }

    private static Value abs(List<Value> values) {
        return applySingleValueFunction("abs", values, BigDecimal::abs);
    }

    private static Value sqrt(List<Value> values) {
        return applySingleValueFunction("sqrt", values, v -> calculateSqrtForAnyDegree(v, BigDecimal.valueOf(2)));
    }

    private static Value root(List<Value> values) {
        return applyDoubleValueFunction("root", values, CoreModuleMath::calculateSqrtForAnyDegree);
    }

    private static BigDecimal calculateSqrtForAnyDegree(BigDecimal value, BigDecimal root) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new MenterExecutionException("sqrt() expects a positive number");
        }
        if (root.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MenterExecutionException("root() expects a positive number as second argument");
        }
        // https://stackoverflow.com/a/34074999/15925251
        BigDecimal xPrev = value;
        BigDecimal x = value.divide(root, Operators.getBigDecimalDivisionScale(), RoundingMode.HALF_DOWN);
        while (x.subtract(xPrev).abs().compareTo(BigDecimal.valueOf(.1).movePointLeft(Operators.getBigDecimalDivisionScale())) > 0) {
            xPrev = x;
            x = root.subtract(BigDecimal.ONE)
                    .multiply(x)
                    .add(value.divide(x.pow(root.intValue() - 1), Operators.getBigDecimalDivisionScale(), RoundingMode.HALF_DOWN))
                    .divide(root, Operators.getBigDecimalDivisionScale(), RoundingMode.HALF_DOWN);
        }
        return x;
    }

    private static Value log(List<Value> values) {
        if (values.size() == 1) {
            return applySingleValueFunction("log", values, v -> {
                if (v.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new MenterExecutionException("log() expects a positive number");
                }
                return log(v, Operators.getBigDecimalDivisionScale());
            });
        } else if (values.size() == 2) {
            throw new MenterExecutionException("log(a, b) is not implemented yet");
        } else {
            throw new MenterExecutionException("log() expects 1 or 2 arguments: log(number), log(number, base)");
        }
    }

    private static Value ln(List<Value> values) {
        return applySingleValueFunction("ln", values, v -> {
            if (v.compareTo(BigDecimal.ZERO) <= 0) {
                throw new MenterExecutionException("ln() expects a positive number");
            }
            throw new MenterExecutionException("ln() is not implemented yet");
        });
    }

    private static final BigDecimal TWO = new BigDecimal(2);
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HALF = ONE.divide(TWO, RoundingMode.HALF_UP);

    public static BigDecimal log(BigDecimal x, int scale) {
        BigDecimal result = BigDecimal.ZERO;
        BigDecimal term = x.subtract(ONE).divide(x.add(ONE), scale, RoundingMode.HALF_UP);
        BigDecimal nextTerm = term;
        for (int i = 1; i < 100; i++) {
            result = result.add(nextTerm.divide(new BigDecimal(2 * i - 1), scale, RoundingMode.HALF_UP));
            nextTerm = nextTerm.multiply(term).multiply(HALF);
            nextTerm = nextTerm.setScale(scale * 4, RoundingMode.HALF_UP);
            if (nextTerm.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
            term = nextTerm;
        }
        return result.multiply(TWO);
    }



}
