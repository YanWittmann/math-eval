package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public abstract class CoreModuleMath {

    static {
        EvaluationContext.registerNativeFunction("math.mtr", "range", CoreModuleMath::range);
        EvaluationContext.registerNativeFunction("math.mtr", "sin", CoreModuleMath::sin);
        EvaluationContext.registerNativeFunction("math.mtr", "cos", CoreModuleMath::cos);
        EvaluationContext.registerNativeFunction("math.mtr", "tan", CoreModuleMath::tan);
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

    public static Value sin(List<Value> arguments) {
        if (arguments.size() != 1) throw new MenterExecutionException("sin() expects 1 argument");
        final Object value = arguments.get(0).getValue();
        if (!(value instanceof BigDecimal)) {
            throw new MenterExecutionException("sin() expects a number as argument");
        }
        return new Value(BigDecimal.valueOf(Math.sin(((BigDecimal) value).doubleValue())));
    }

    public static Value cos(List<Value> arguments) {
        if (arguments.size() != 1) throw new MenterExecutionException("cos() expects 1 argument");
        final Object value = arguments.get(0).getValue();
        if (!(value instanceof BigDecimal)) {
            throw new MenterExecutionException("cos() expects a number as argument");
        }
        return new Value(BigDecimal.valueOf(Math.cos(((BigDecimal) value).doubleValue())));
    }

    public static Value tan(List<Value> arguments) {
        if (arguments.size() != 1) throw new MenterExecutionException("tan() expects 1 argument");
        final Object value = arguments.get(0).getValue();
        if (!(value instanceof BigDecimal)) {
            throw new MenterExecutionException("tan() expects a number as argument");
        }
        return new Value(BigDecimal.valueOf(Math.tan(((BigDecimal) value).doubleValue())));
    }
}
