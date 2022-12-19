package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.lexer.Token;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Value {

    private Object value;
    private Value secondaryValue;

    public Value(Object value) {
        this(value, null);
    }

    public Value(Object value, Value secondaryValue) {
        if (value instanceof Integer) value = new BigDecimal((Integer) value);
        else if (value instanceof Long) value = new BigDecimal((Long) value);
        else if (value instanceof Float) value = BigDecimal.valueOf((Float) value);
        else if (value instanceof Double) value = BigDecimal.valueOf((Double) value);

        this.value = value;
        this.secondaryValue = secondaryValue;
    }

    public Object getValue() {
        return value;
    }

    public Value getSecondaryValue() {
        return secondaryValue;
    }

    public void inheritValue(Value value) {
        if (value == null) {
            throw new MenterExecutionException("Cannot inherit value from null");
        }
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
        } else if (value instanceof MFunction) {
            return PrimitiveValueType.FUNCTION.getType();
        } else if (value instanceof BiFunction) {
            return PrimitiveValueType.VALUE_FUNCTION.getType();
        } else if (value instanceof Function) {
            return PrimitiveValueType.NATIVE_FUNCTION.getType();
        } else {
            return "unknown";
        }
    }

    public boolean isFunction() {
        final String type = getType();
        return type.equals(PrimitiveValueType.FUNCTION.getType()) ||
               type.equals(PrimitiveValueType.VALUE_FUNCTION.getType()) ||
               type.equals(PrimitiveValueType.NATIVE_FUNCTION.getType());
    }

    public BigDecimal getNumberValue() {
        if (Objects.equals(this.getType(), PrimitiveValueType.NUMBER.getType())) {
            return (BigDecimal) value;
        } else {
            return null;
        }
    }

    public Value access(Value identifier) {
        if (identifier.getValue() == null) {
            return Value.empty();
        }

        if (VALUE_FUNCTIONS.containsKey(this.getType()) && VALUE_FUNCTIONS.get(this.getType()).containsKey(String.valueOf(identifier.getValue()))) {
            return new Value(VALUE_FUNCTIONS.get(this.getType()).get(identifier.getValue().toString()), this);
        }

        if (this.getType().equals(PrimitiveValueType.OBJECT.getType())) {
            return ((Map<String, Value>) value).get(identifier.getValue());
        } else if (this.getType().equals(PrimitiveValueType.ARRAY.getType())) {
            return ((List<Value>) value).get(identifier.getNumberValue().intValue());
        }

        return null;
    }

    public Value create(Value identifier, Value value) {
        if (this.getType().equals(PrimitiveValueType.OBJECT.getType())) {
            ((Map<String, Value>) this.value).put(identifier.getValue().toString(), value);
        } else if (this.getType().equals(PrimitiveValueType.ARRAY.getType())) {
            ((List<Value>) this.value).add(value);
        }
        return value;
    }

    private final static Map<String, Map<String, java.util.function.BiFunction<Value, List<Value>, Value>>> VALUE_FUNCTIONS = new HashMap<String, Map<String, java.util.function.BiFunction<Value, List<Value>, Value>>>() {
        {
            put(PrimitiveValueType.ARRAY.getType(), new HashMap<String, java.util.function.BiFunction<Value, List<Value>, Value>>() {
                {
                    put("size", (self, values) -> new Value(((List<?>) self.getValue()).size()));
                }
            });
        }
    };

    @Override
    public String toString() {
        return value + " (" + getType() + ")";
    }

    public String toDisplayString() {
        return toDisplayString(value);
    }

    public static String toDisplayString(Object object) {
        if (object instanceof Value) {
            return toDisplayString(((Value) object).getValue());

        } else if (object instanceof List) {
            return ((List<?>) object).stream()
                    .map(v -> v instanceof Value ? ((Value) v).toDisplayString() : v)
                    .collect(Collectors.toList())
                    .toString();

        } else if (object instanceof Map) {
            final StringJoiner joiner = new StringJoiner(", ", "{", "}");
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
                joiner.add(toDisplayString(entry.getKey()) + ": " + toDisplayString(entry.getValue()));
            }
            return joiner.toString();

        } else if (object instanceof Token) {
            return ((Token) object).getValue();

        } else if (object instanceof Pattern) {
            return ((Pattern) object).pattern();

        } else if (object instanceof BigDecimal) {
            return ((BigDecimal) object).stripTrailingZeros().toPlainString();

        } else if (object instanceof String) {
            return "\"" + object + "\"";

        } else {
            return String.valueOf(object);
        }
    }

    public boolean isEmpty() {
        return value == null;
    }

    public static Value empty() {
        return new Value(null);
    }
}
