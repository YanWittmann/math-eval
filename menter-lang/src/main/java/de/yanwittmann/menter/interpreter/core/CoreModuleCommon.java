package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CoreModuleCommon {

    public static Value range(Value[] arguments) {
        if (arguments.length < 2) throw new MenterExecutionException("range() expects 2 or 3 arguments");
        final Object start = arguments[0].getValue();
        final Object end = arguments[1].getValue();
        final Object stepSizeParam = arguments.length == 3 ? arguments[2].getValue() : BigDecimal.ONE;

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

    public static Value print(Value[] arguments) {
        MenterDebugger.printer.println(Arrays.stream(arguments)
                .map(v -> v.toDisplayString())
                .collect(Collectors.joining(" ")));
        return Value.empty();
    }
}
