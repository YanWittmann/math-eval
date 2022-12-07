package de.yanwittmann.matheval.interpreter.structure;

import java.util.regex.Pattern;

public class Value {

    private Object value;
    private final String type;

    protected Value(Object value, String type) {
        this.value = value;
        this.type = type;
    }

    protected Value(Object value, PrimitiveValueType type) {
        this.value = value;
        this.type = type.getType();
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public static Value of(Object value) {
        final Object type;

        if (value instanceof Number) {
            type = PrimitiveValueType.NUMBER;
        } else if (value instanceof String) {
            type = PrimitiveValueType.STRING;
        } else if (value instanceof Boolean) {
            type = PrimitiveValueType.BOOLEAN;
        } else if (value instanceof Pattern) {
            type = PrimitiveValueType.REGEX;
        } else {
            type = "unknown";
        }

        if (type instanceof PrimitiveValueType) {
            return new Value(value, (PrimitiveValueType) type);
        } else {
            return new Value(value, (String) type);
        }
    }
}
