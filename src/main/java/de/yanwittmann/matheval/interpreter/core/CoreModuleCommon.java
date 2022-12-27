package de.yanwittmann.matheval.interpreter.core;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.interpreter.structure.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CoreModuleCommon {

    public static Value range(Value[] arguments) {
        if (arguments.length != 2) throw new MenterExecutionException("range() expects 2 arguments");
        final Object start = arguments[0].getValue();
        final Object end = arguments[1].getValue();

        if (start instanceof BigDecimal && end instanceof BigDecimal) {
            final BigDecimal startDecimal = (BigDecimal) start;
            final BigDecimal endDecimal = (BigDecimal) end;
            final List<Value> values = new ArrayList<>();
            for (BigDecimal i = startDecimal; i.compareTo(endDecimal) <= 0; i = i.add(BigDecimal.ONE)) {
                values.add(new Value(i));
            }
            return new Value(values);
        } else if (start instanceof String && end instanceof String) {
            final String startString = (String) start;
            final String endString = (String) end;
            final List<Value> values = new ArrayList<>();
            for (char i = startString.charAt(0); i <= endString.charAt(0); i++) {
                values.add(new Value(String.valueOf(i)));
            }
            return new Value(values);
        } else {
            throw new MenterExecutionException("range() expects 2 numbers or 2 one-character strings");
        }
    }

    public static Value print(Value[] arguments) {
        System.out.println(Arrays.stream(arguments)
                .map(v -> v.toDisplayString())
                .collect(Collectors.joining(" ")));
        return Value.empty();
    }

}
