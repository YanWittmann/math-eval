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
        else if (value instanceof List) {
            final Map<BigDecimal, Value> map = new HashMap<>();
            int i = 0;
            for (Object o : (List<?>) value) {
                map.put(new BigDecimal(i++), new Value(o));
            }
            value = map;
        } else if (value instanceof Value) {
            value = ((Value) value).getValue();
        }

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
        } else if (value instanceof LinkedHashMap) {
            return PrimitiveValueType.OBJECT.getType();
        } else if (value instanceof HashMap) {
            return PrimitiveValueType.OBJECT.getType();
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
        if (VALUE_FUNCTIONS.get(PrimitiveValueType.UNKNOWN.getType()).containsKey(String.valueOf(identifier.getValue()))) {
            return new Value(VALUE_FUNCTIONS.get(PrimitiveValueType.UNKNOWN.getType()).get(identifier.getValue().toString()), this);
        }

        if (this.getType().equals(PrimitiveValueType.OBJECT.getType()) || this.getType().equals(PrimitiveValueType.ARRAY.getType())) {
            return ((Map<Object, Value>) value).get(identifier.getValue());
        }

        return null;
    }

    public Value create(Value identifier, Value value) {
        if (this.getType().equals(PrimitiveValueType.OBJECT.getType()) || this.getType().equals(PrimitiveValueType.ARRAY.getType())) {
            ((Map<Object, Value>) this.value).put(identifier.getValue(), value);
        }

        return value;
    }

    private final static Map<String, Map<String, java.util.function.BiFunction<Value, List<Value>, Value>>> VALUE_FUNCTIONS = new HashMap<String, Map<String, java.util.function.BiFunction<Value, List<Value>, Value>>>() {
        {
            put(PrimitiveValueType.OBJECT.getType(), new HashMap<String, java.util.function.BiFunction<Value, List<Value>, Value>>() {
                {
                    put("size", (self, values) -> new Value(((Map<?, ?>) self.getValue()).size()));
                    put("keys", (self, values) -> new Value(((Map<?, ?>) self.getValue()).keySet().stream().map(Value::new).collect(Collectors.toList())));
                    put("values", (self, values) -> new Value(((Map<?, ?>) self.getValue()).values().stream().map(Value::new).collect(Collectors.toList())));
                    put("entries", (self, values) -> new Value(((Map<?, ?>) self.getValue()).entrySet().stream().map(entry -> new Value(new LinkedHashMap<Object, Value>() {{
                        put("key", new Value(entry.getKey()));
                        put("value", ((Value) entry.getValue()));
                    }})).collect(Collectors.toList())));
                }
            });
            put(PrimitiveValueType.UNKNOWN.getType(), new HashMap<String, java.util.function.BiFunction<Value, List<Value>, Value>>() {
                {
                    put("type", (self, values) -> new Value(self.getType()));
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
            final Map<?, ?> map = (Map<?, ?>) object;
            if (map.keySet().stream().allMatch(k -> k instanceof BigDecimal)) {
                return toDisplayString(map.values().stream().map(v -> v instanceof Value ? ((Value) v).toDisplayString() : v).collect(Collectors.toList()));
            } else {
                final StringJoiner joiner = new StringJoiner(", ", "{", "}");
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    joiner.add(toDisplayString(entry.getKey()) + ": " + toDisplayString(entry.getValue()));
                }
                return joiner.toString();
            }

        } else if (object instanceof Token) {
            return ((Token) object).getValue();
        } else if (object instanceof Pattern) {
            return ((Pattern) object).pattern();
        } else if (object instanceof BigDecimal) {
            return ((BigDecimal) object).stripTrailingZeros().toPlainString();
        } else if (object instanceof String) {
            return "\"" + object + "\"";

        } else if (object instanceof Function) {
            return "<<native function>>";
        } else if (object instanceof BiFunction) {
            return "<<instance function>>";
        } else if (object instanceof MFunction) {
            return String.valueOf(object);

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

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
