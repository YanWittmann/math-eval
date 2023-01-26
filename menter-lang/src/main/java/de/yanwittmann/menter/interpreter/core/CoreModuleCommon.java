package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CoreModuleCommon {

    static {
        EvaluationContext.registerNativeFunction("common.mtr", "print", CoreModuleCommon::print);
        EvaluationContext.registerNativeFunction("common.mtr", "range", CoreModuleCommon::range);
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

    public static Value print(List<Value> arguments) {
        MenterDebugger.printer.println(arguments.stream()
                .map(v -> v.toDisplayString())
                .collect(Collectors.joining(" ")));
        return Value.empty();
    }
}
