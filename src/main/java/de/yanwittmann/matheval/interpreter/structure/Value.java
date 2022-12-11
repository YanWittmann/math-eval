package de.yanwittmann.matheval.interpreter.structure;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Value {

    private Object value;

    public Value(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void inheritValue(Value value) {
        this.value = value.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getType() {
        if (value == null) {
            return "unknown";
        } else if (value instanceof Number) {
            return PrimitiveValueType.NUMBER.getType();
        } else if (value instanceof String) {
            return PrimitiveValueType.STRING.getType();
        } else if (value instanceof Boolean) {
            return PrimitiveValueType.BOOLEAN.getType();
        } else if (value instanceof Pattern) {
            return PrimitiveValueType.REGEX.getType();
        } else if (value instanceof Map) {
            return PrimitiveValueType.OBJECT.getType();
        } else if (value instanceof List) {
            return PrimitiveValueType.ARRAY.getType();
        } else if (value instanceof Function) {
            return PrimitiveValueType.FUNCTION.getType();
        } else {
            return "unknown";
        }
    }

    public Value access(Value identifier) {
        if (getType().equals(PrimitiveValueType.OBJECT.getType())) {
            return ((Map<String, Value>) value).get(identifier.getValue());
        } else if (getType().equals(PrimitiveValueType.ARRAY.getType())) {
            return ((List<Value>) value).get(Integer.parseInt(identifier.getValue().toString()));
        }

        return null;
    }

    public Value create(Value identifier, Value value) {
        if (getType().equals(PrimitiveValueType.OBJECT.getType())) {
            ((Map<String, Value>) this.value).put(identifier.getValue().toString(), value);
        } else if (getType().equals(PrimitiveValueType.ARRAY.getType())) {
            ((List<Value>) this.value).add(value);
        }
        return value;
    }

    @Override
    public String toString() {
        return value + " (" + getType() + ")";
    }
}
