package de.yanwittmann.matheval.interpreter.structure;

import java.util.regex.Pattern;

public class Value<T> {

    private T value;
    private final String type;

    protected Value(T value, String type) {
        this.value = value;
        this.type = type;
    }

    protected Value(T value, ValueType type) {
        this.value = value;
        this.type = type.getType();
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public static Value<?> of(Object value) {
        if (value instanceof Number) {
            return new Value<>((Number) value, ValueType.NUMBER);
        } else if (value instanceof String) {
            return new Value<>((String) value, ValueType.STRING);
        } else if (value instanceof Boolean) {
            return new Value<>((Boolean) value, ValueType.BOOLEAN);
        } else if (value instanceof Pattern) {
            return new Value<>((Pattern) value, ValueType.REGEX);
        } else {
            return new Value<>(value, "unknown");
        }
    }
}
