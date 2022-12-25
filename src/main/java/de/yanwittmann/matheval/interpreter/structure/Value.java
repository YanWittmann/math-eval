package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.lexer.Token;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Value implements Comparable<Value> {

    private Object value;
    private Value secondaryValue;

    public Value(Object value) {
        this(value, null);
    }

    public Value(Object value, Value secondaryValue) {
        setValue(value);
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
        if (value instanceof Integer) this.value = new BigDecimal((Integer) value);
        else if (value instanceof Long) this.value = new BigDecimal((Long) value);
        else if (value instanceof Float) this.value = BigDecimal.valueOf((Float) value);
        else if (value instanceof Double) this.value = BigDecimal.valueOf((Double) value);
        else if (value instanceof List) {
            final Map<Object, Value> map = new LinkedHashMap<>();
            int i = 0;
            for (Object o : (List<?>) value) {
                map.put(new BigDecimal(i++), new Value(o));
            }
            this.value = map;
        } else if (value instanceof Value) {
            inheritValue((Value) value);
        } else if (value instanceof Map.Entry) {
            final Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) value;
            final Map<Object, Value> map = new LinkedHashMap<>();
            map.put("key", new Value(mapEntry.getKey()));
            map.put("value", new Value(mapEntry.getValue()));
            this.value = map;
        } else if (value instanceof Token) {
            setValue(((Token) value).getValue());
        } else {
            this.value = value;
        }
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
        } else if (value instanceof MNodeFunction) {
            return PrimitiveValueType.FUNCTION.getType();
        } else if (value instanceof MValueFunction) {
            return PrimitiveValueType.VALUE_FUNCTION.getType();
        } else if (value instanceof Function) {
            return PrimitiveValueType.NATIVE_FUNCTION.getType();
        } else if (value instanceof Iterator<?>) {
            return PrimitiveValueType.ITERATOR.getType();
        } else {
            return value.getClass().getSimpleName();
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

    public boolean isTrue() {
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        } else if (value instanceof String) {
            return !((String) value).isEmpty();
        } else if (value instanceof LinkedHashMap) {
            return !((LinkedHashMap<?, ?>) value).isEmpty();
        } else if (value instanceof HashMap) {
            return !((HashMap<?, ?>) value).isEmpty();
        }

        return value instanceof MNodeFunction || value instanceof MValueFunction ||
               value instanceof Function || value instanceof Pattern;
    }

    public int size() {
        if (value instanceof Map) {
            return ((Map<?, ?>) value).size();
        } else if (value instanceof String) {
            return ((String) value).length();
        } else if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        } else if (value instanceof Iterator) {
            int i = 0;
            while (((Iterator<?>) value).hasNext()) {
                ((Iterator<?>) value).next();
                i++;
            }
            return i;
        } else {
            return 0;
        }
    }

    public Value access(Value identifier) {
        if (identifier.getValue() == null) {
            return Value.empty();
        }

        if (VALUE_FUNCTIONS.containsKey(this.getType()) && VALUE_FUNCTIONS.get(this.getType()).containsKey(String.valueOf(identifier.getValue()))) {
            return new Value(VALUE_FUNCTIONS.get(this.getType()).get(identifier.getValue().toString()), this);
        }
        if (VALUE_FUNCTIONS.get(PrimitiveValueType.ANY.getType()).containsKey(String.valueOf(identifier.getValue()))) {
            return new Value(VALUE_FUNCTIONS.get(PrimitiveValueType.ANY.getType()).get(identifier.getValue().toString()), this);
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

    private final static Map<String, Map<String, MValueFunction>> VALUE_FUNCTIONS = new HashMap<String, Map<String, MValueFunction>>() {
        {
            put(PrimitiveValueType.OBJECT.getType(), new HashMap<String, MValueFunction>() {
                {
                    put("size", (context, self, values, localSymbols) -> new Value(self.size()));
                    put("keys", (context, self, values, localSymbols) -> new Value(((Map<?, ?>) self.getValue()).keySet().stream().map(Value::new).collect(Collectors.toList())));
                    put("values", (context, self, values, localSymbols) -> new Value(((Map<?, ?>) self.getValue()).values().stream().map(Value::new).collect(Collectors.toList())));
                    put("entries", (context, self, values, localSymbols) -> new Value(((Map<?, ?>) self.getValue()).entrySet().stream().map(entry -> new Value(new LinkedHashMap<Object, Value>() {{
                        put("key", new Value(entry.getKey()));
                        put("value", ((Value) entry.getValue()));
                    }})).collect(Collectors.toList())));

                    put("containsKey", (context, self, values, localSymbols) -> new Value(((Map<?, ?>) self.getValue()).containsKey(values.get(0).getValue())));
                    put("containsValue", (context, self, values, localSymbols) -> new Value(((Map<?, ?>) self.getValue()).containsValue(values.get(0).getValue())));

                    put("forEach", (context, self, values, localSymbols) -> {
                        for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                            applyFunction(toList(entry.getKey(), entry.getValue()), values.get(0), context, localSymbols);
                        }
                        return Value.empty();
                    });

                    put("map", (context, self, values, localSymbols) -> {
                        final Map<Object, Value> map = new LinkedHashMap<>();
                        for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                            map.put(entry.getKey(), applyFunction(toList(entry.getValue()), values.get(0), context, localSymbols));
                        }
                        return new Value(map);
                    });
                    put("mapKeys", (context, self, values, localSymbols) -> {
                        final Map<Object, Value> map = new LinkedHashMap<>();
                        for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                            map.put(applyFunction(toList(entry.getKey()), values.get(0), context, localSymbols).getValue(), entry.getValue());
                        }
                        return new Value(map);
                    });

                    put("filter", (context, self, values, localSymbols) -> {
                        if (isMapAnArray(((Map<Object, Value>) self.getValue()))) {
                            final List<Value> mapped = new ArrayList<>();
                            for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                                if (applyFunction(toList(entry.getValue()), values.get(0), context, localSymbols).isTrue()) {
                                    mapped.add(entry.getValue());
                                }
                            }
                            return new Value(mapped);

                        } else {
                            final Map<Object, Value> map = new LinkedHashMap<>();
                            for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                                if (applyFunction(toList(entry.getValue()), values.get(0), context, localSymbols).isTrue()) {
                                    map.put(entry.getKey(), entry.getValue());
                                }
                            }
                            return new Value(map);
                        }
                    });
                    put("filterKeys", (context, self, values, localSymbols) -> {
                        final Map<Object, Value> map = new LinkedHashMap<>();
                        for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                            if (applyFunction(toList(entry.getKey()), values.get(0), context, localSymbols).isTrue()) {
                                map.put(entry.getKey(), entry.getValue());
                            }
                        }
                        return new Value(map);
                    });

                    put("distinct", (context, self, values, localSymbols) -> {
                        if (isMapAnArray(((Map<Object, Value>) self.getValue()))) {
                            final List<Value> mapped = new ArrayList<>();
                            for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                                if (!mapped.contains(entry.getValue())) {
                                    mapped.add(entry.getValue());
                                }
                            }
                            return new Value(mapped);

                        } else {
                            final Map<Object, Value> map = new LinkedHashMap<>();
                            for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                                if (!map.containsValue(entry.getValue())) {
                                    map.put(entry.getKey(), entry.getValue());
                                }
                            }
                            return new Value(map);
                        }
                    });

                    put("sort", (context, self, values, localSymbols) -> {
                        final Comparator<Value> comparator;
                        if (values.size() > 0) {
                            final List<String> parameterList = getParameterList(values.get(0));
                            if (parameterList == null || parameterList.size() == 2) {
                                comparator = (a, b) -> {
                                    final Value result = applyFunction(toList(a, b), values.get(0), context, localSymbols);
                                    if (result.getType().equals(PrimitiveValueType.NUMBER.getType())) {
                                        return ((Number) result.getValue()).intValue();
                                    }
                                    return 0;
                                };
                            } else {
                                comparator = Value::compareTo;
                            }
                        } else {
                            comparator = Value::compareTo;
                        }

                        if (isMapAnArray(((Map<Object, Value>) self.getValue()))) {
                            return new Value(((Map<Object, Value>) self.getValue()).values().stream()
                                    .sorted(comparator)
                                    .collect(Collectors.toList()));
                        } else {
                            return new Value(((Map<Object, Value>) self.getValue()).entrySet().stream()
                                    .sorted((a, b) -> comparator.compare(a.getValue(), b.getValue()))
                                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
                        }
                    });

                    put("join", (context, self, values, localSymbols) -> {
                        final String separator = values.size() > 0 ? values.get(0).toDisplayString() : "";
                        final String prefix = values.size() > 1 ? values.get(1).toDisplayString() : "";
                        final String suffix = values.size() > 2 ? values.get(2).toDisplayString() : "";
                        final StringBuilder sb = new StringBuilder();
                        sb.append(prefix);
                        for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                            if (sb.length() > prefix.length()) {
                                sb.append(separator);
                            }
                            sb.append(entry.getValue().toDisplayString());
                        }
                        sb.append(suffix);
                        return new Value(sb.toString());
                    });

                    put("iterator", (context, self, values, localSymbols) -> makeIteratorValueIterator(((Map<Object, Value>) self.getValue()).entrySet().iterator()));
                }
            });
            put(PrimitiveValueType.STRING.getType(), new HashMap<String, MValueFunction>() {
                {
                    put("size", (context, self, values, localSymbols) -> new Value(self.size()));
                    put("charAt", (context, self, values, localSymbols) -> new Value(((String) self.getValue()).charAt(values.get(0).getNumberValue().intValue())));

                    put("iterator", (context, self, values, localSymbols) -> makeIteratorValueIterator(((String) self.getValue()).chars().mapToObj(c -> (char) c).iterator()));
                }
            });
            put(PrimitiveValueType.ITERATOR.getType(), new HashMap<String, MValueFunction>() {
                {
                    put("hasNext", (context, self, values, localSymbols) -> new Value(((Iterator<?>) self.getValue()).hasNext()));
                    put("next", (context, self, values, localSymbols) -> new Value(((Iterator<?>) self.getValue()).next()));

                    put("forEach", (context, self, values, localSymbols) -> {
                        final Iterator<?> iterator = (Iterator<?>) self.getValue();
                        while (iterator.hasNext()) {
                            applyFunction(toList(new Value(iterator.next())), values.get(0), context, localSymbols);
                        }
                        return self;
                    });

                    put("iterator", (context, self, values, localSymbols) -> self);
                }
            });
            put(PrimitiveValueType.ANY.getType(), new HashMap<String, MValueFunction>() {
                {
                    put("type", (context, self, values, localSymbols) -> new Value(self.getType()));
                }
            });
        }
    };

    private static Value makeIteratorValueIterator(Iterator<?> iterator) {
        return new Value(new Iterator<Value>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Value next() {
                return new Value(iterator.next());
            }
        });
    }

    private static Value applyFunction(List<Value> value, Value function, GlobalContext context, Map<String, Value> localSymbols) {
        return context.evaluateFunction(function, value, context, localSymbols);
    }

    private static List<String> getParameterList(Value value) {
        if (!value.isFunction()) {
            throw new MenterExecutionException("Value is not a function " + value);
        }

        if (value.getValue() instanceof MNodeFunction) {
            final MNodeFunction executableFunction = (MNodeFunction) value.getValue();
            return executableFunction.getArgumentNames();
        } else {
            return null;
        }
    }

    private static List<Value> toList(Object... value) {
        final List<Value> list = new ArrayList<>();
        for (Object object : value) {
            list.add(new Value(object));
        }
        return list;
    }

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
            if (isMapAnArray(map)) {
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

        } else if (object instanceof Function) {
            return "<<native function>>";
        } else if (object instanceof BiFunction) {
            return "<<instance function>>";
        } else if (object instanceof MNodeFunction) {
            return String.valueOf(object);

        } else if (object instanceof Iterator) {
            // cannot print the iterator, because it will consume the values
            return "<<iterator>>";

        } else {
            return String.valueOf(object);
        }
    }

    private static boolean isMapAnArray(Map<?, ?> map) {
        if (!map.keySet().stream().allMatch(k -> k instanceof BigDecimal)) {
            return false;
        }

        if (!map.containsKey(BigDecimal.ZERO)) {
            return false;
        }

        return map.keySet().stream().map(k -> (BigDecimal) k).max(BigDecimal::compareTo).get().intValue() == map.size() - 1;
    }

    public boolean isEmpty() {
        return value == null;
    }

    public static Value empty() {
        return new Value(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Value)) {
            return false;
        }

        final Value other = (Value) obj;
        if (!this.getType().equals(other.getType())) {
            return false;
        }

        if (this.getType().equals(PrimitiveValueType.NUMBER.getType())) {
            return this.getNumberValue().compareTo(other.getNumberValue()) == 0;
        } else if (this.getType().equals(PrimitiveValueType.BOOLEAN.getType())) {
            return this.isTrue() == other.isTrue();
        }

        return this.getValue().equals(other.getValue());
    }

    @Override
    public int compareTo(Value o) {
        if (this.getType().equals(PrimitiveValueType.NUMBER.getType())) {
            return this.getNumberValue().compareTo(o.getNumberValue());
        } else if (this.getType().equals(PrimitiveValueType.BOOLEAN.getType())) {
            return Boolean.compare(this.isTrue(), o.isTrue());
        } else if (this.getType().equals(PrimitiveValueType.STRING.getType())) {
            return this.getValue().toString().compareTo(o.getValue().toString());
        } else if (this.getType().equals(PrimitiveValueType.ARRAY.getType())) {
            return Integer.compare(((Map<?, ?>) this.getValue()).size(), ((Map<?, ?>) o.getValue()).size());
        } else if (this.getType().equals(PrimitiveValueType.OBJECT.getType())) {
            return Integer.compare(((Map<?, ?>) this.getValue()).size(), ((Map<?, ?>) o.getValue()).size());
        } else {
            return Integer.compare(this.getValue().hashCode(), o.getValue().hashCode());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
