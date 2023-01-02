package de.yanwittmann.menter.interpreter.structure;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.MenterInterpreter;
import de.yanwittmann.menter.lexer.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Value implements Comparable<Value> {

    private final static Logger LOG = LogManager.getLogger(Value.class);

    private final static List<CustomValueType> CUSTOM_VALUE_TYPES = new ArrayList<>();

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

    public LinkedHashMap<String, Value> getMap() {
        if (value instanceof LinkedHashMap) return (LinkedHashMap<String, Value>) value;
        else throw new MenterExecutionException("Cannot transform type " + getType() + " to map");
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
        } else if (value instanceof Map && !(value instanceof LinkedHashMap)) {
            this.value = new LinkedHashMap<>((Map<?, ?>) value);
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
        } else if (value instanceof BigDecimal) {
            this.value = ((BigDecimal) value).stripTrailingZeros();
        } else {
            this.value = value;
        }
    }

    public String getType() {
        if (value == null) {
            return "empty";
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
        } else if (value instanceof MenterNodeFunction) {
            return PrimitiveValueType.FUNCTION.getType();
        } else if (value instanceof MenterValueFunction) {
            return PrimitiveValueType.VALUE_FUNCTION.getType();
        } else if (value instanceof Function) {
            return PrimitiveValueType.NATIVE_FUNCTION.getType();
        } else if (value instanceof Iterator<?>) {
            return PrimitiveValueType.ITERATOR.getType();
        } else {
            for (CustomValueType type : CUSTOM_VALUE_TYPES) {
                if (type.isType(value)) {
                    return type.getType();
                }
            }

            return value.getClass().getSimpleName();
        }
    }

    public boolean isFunction() {
        final String type = getType();
        return type.equals(PrimitiveValueType.FUNCTION.getType()) ||
               type.equals(PrimitiveValueType.VALUE_FUNCTION.getType()) ||
               type.equals(PrimitiveValueType.NATIVE_FUNCTION.getType());
    }

    public BigDecimal getNumericValue() {
        if (Objects.equals(this.getType(), PrimitiveValueType.NUMBER.getType())) {
            return (BigDecimal) value;
        } else {
            for (CustomValueType type : CUSTOM_VALUE_TYPES) {
                if (type.isType(value)) {
                    return type.getNumericValue(this);
                }
            }

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

        if (value instanceof MenterNodeFunction || value instanceof MenterValueFunction ||
            value instanceof Function || value instanceof Pattern) {
            return true;
        }

        for (CustomValueType type : CUSTOM_VALUE_TYPES) {
            if (type.isType(value)) {
                return type.isTrue(this);
            }
        }

        return false;
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
            for (CustomValueType type : CUSTOM_VALUE_TYPES) {
                if (type.isType(value)) {
                    return type.size(this);
                }
            }

            return 1;
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

        for (CustomValueType type : CUSTOM_VALUE_TYPES) {
            if (type.isType(this.value)) {
                return type.accessValue(this, identifier);
            }
        }

        return null;
    }

    public List<Map.Entry<String, Map<String, MenterValueFunction>>> getValueFunctionCandidates() {
        final List<Map.Entry<String, Map<String, MenterValueFunction>>> candidates = new ArrayList<>();
        if (VALUE_FUNCTIONS.containsKey(this.getType())) {
            candidates.add(new AbstractMap.SimpleEntry<>(this.getType(), VALUE_FUNCTIONS.get(this.getType())));
        }
        if (VALUE_FUNCTIONS.containsKey(PrimitiveValueType.ANY.getType())) {
            candidates.add(new AbstractMap.SimpleEntry<>(PrimitiveValueType.ANY.getType(), VALUE_FUNCTIONS.get(PrimitiveValueType.ANY.getType())));
        }
        return candidates;
    }

    public boolean create(Value identifier, Value value, boolean isFinalIdentifier) {
        if (this.getType().equals(PrimitiveValueType.OBJECT.getType()) || this.getType().equals(PrimitiveValueType.ARRAY.getType())) {
            if (!isFinalIdentifier) {
                value.setValue(new LinkedHashMap<>());
            }
            ((Map<Object, Value>) this.value).put(identifier.getValue(), value);
            return true;
        }

        for (CustomValueType type : CUSTOM_VALUE_TYPES) {
            if (type.isType(this.value)) {
                return type.createAccessedValue(this, identifier, value, isFinalIdentifier);
            }
        }

        return false;
    }

    public static void registerCustomValueType(MenterInterpreter interpreter, CustomValueType type) {
        final String typename = type.getType();
        final HashMap<String, MenterValueFunction> functions = type.getFunctions();

        if (VALUE_FUNCTIONS.containsKey(typename)) {
            LOG.warn("Overwriting custom value type [{}], appending/overwriting functions", typename);
            VALUE_FUNCTIONS.get(typename).putAll(functions);
        } else {
            VALUE_FUNCTIONS.put(typename, functions);
        }

        CUSTOM_VALUE_TYPES.add(type);

        final String contextName = typename + "_" + new Random(typename.hashCode() + type.hashCode()).ints(48, 122)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(6)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        interpreter.evaluateInContextOf(type.contextSource(), contextName);
    }

    public static void removeCustomValueType(CustomValueType type) {
        final String typename = type.getType();
        final HashMap<String, MenterValueFunction> functions = type.getFunctions();

        if (VALUE_FUNCTIONS.containsKey(typename)) {
            VALUE_FUNCTIONS.get(typename).keySet().removeAll(functions.keySet());
        }

        CUSTOM_VALUE_TYPES.remove(type);
    }

    private final static Map<String, Map<String, MenterValueFunction>> VALUE_FUNCTIONS = new HashMap<String, Map<String, MenterValueFunction>>() {
        {
            put(PrimitiveValueType.OBJECT.getType(), new HashMap<String, MenterValueFunction>() {
                {
                    put("size", (context, self, values, localInformation) -> new Value(self.size()));
                    put("keys", (context, self, values, localInformation) -> new Value(((Map<?, ?>) self.getValue()).keySet().stream().map(Value::new).collect(Collectors.toList())));
                    put("values", (context, self, values, localInformation) -> new Value(((Map<?, ?>) self.getValue()).values().stream().map(Value::new).collect(Collectors.toList())));
                    put("entries", (context, self, values, localInformation) -> new Value(((Map<?, ?>) self.getValue()).entrySet().stream().map(entry -> new Value(new LinkedHashMap<Object, Value>() {{
                        put("key", new Value(entry.getKey()));
                        put("value", ((Value) entry.getValue()));
                    }})).collect(Collectors.toList())));

                    put("containsValue", (context, self, values, localInformation) -> new Value(((Map<?, ?>) self.getValue()).values().stream().anyMatch(value -> value.equals(values.get(0)))));
                    put("contains", (context, self, values, localInformation) -> new Value(((Map<?, ?>) self.getValue()).values().stream().anyMatch(value -> value.equals(values.get(0)))));
                    put("containsKey", (context, self, values, localInformation) -> new Value(((Map<?, ?>) self.getValue()).keySet().stream().anyMatch(key -> key.equals(values.get(0).getValue()))));

                    put("forEach", (context, self, values, localInformation) -> {
                        final Map<Object, Value> mapValue = (Map<Object, Value>) self.getValue();
                        final boolean mapAnArray = isMapAnArray(mapValue);
                        for (Entry<Object, Value> entry : mapValue.entrySet()) {
                            if (mapAnArray) {
                                try {
                                    applyFunction(toList(entry.getValue()), values.get(0), context, localInformation, "forEach");
                                } catch (Exception e) {
                                    try {
                                        applyFunction(toList(entry.getKey(), entry.getValue()), values.get(0), context, localInformation, "forEach");
                                    } catch (Exception ignored) {
                                        throw e;
                                    }
                                }
                            } else {
                                applyFunction(toList(entry.getKey(), entry.getValue()), values.get(0), context, localInformation, "forEach");
                            }
                        }
                        return Value.empty();
                    });

                    put("map", (context, self, values, localInformation) -> {
                        final Map<Object, Value> map = new LinkedHashMap<>();
                        for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                            map.put(entry.getKey(), applyFunction(toList(entry.getValue()), values.get(0), context, localInformation, "map"));
                        }
                        return new Value(map);
                    });
                    put("mapKeys", (context, self, values, localInformation) -> {
                        final Map<Object, Value> map = new LinkedHashMap<>();
                        for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                            map.put(applyFunction(toList(entry.getKey()), values.get(0), context, localInformation, "mapKeys").getValue(), entry.getValue());
                        }
                        return new Value(map);
                    });

                    put("filter", (context, self, values, localInformation) -> {
                        if (isMapAnArray(((Map<Object, Value>) self.getValue()))) {
                            final List<Value> mapped = new ArrayList<>();
                            for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                                if (applyFunction(toList(entry.getValue()), values.get(0), context, localInformation, "filter").isTrue()) {
                                    mapped.add(entry.getValue());
                                }
                            }
                            return new Value(mapped);

                        } else {
                            final Map<Object, Value> map = new LinkedHashMap<>();
                            for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                                if (applyFunction(toList(entry.getValue()), values.get(0), context, localInformation, "filter").isTrue()) {
                                    map.put(entry.getKey(), entry.getValue());
                                }
                            }
                            return new Value(map);
                        }
                    });
                    put("filterKeys", (context, self, values, localInformation) -> {
                        final Map<Object, Value> map = new LinkedHashMap<>();
                        for (Entry<Object, Value> entry : ((Map<Object, Value>) self.getValue()).entrySet()) {
                            if (applyFunction(toList(entry.getKey()), values.get(0), context, localInformation, "filterKeys").isTrue()) {
                                map.put(entry.getKey(), entry.getValue());
                            }
                        }
                        return new Value(map);
                    });

                    put("distinct", (context, self, values, localInformation) -> {
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

                    put("sort", (context, self, values, localInformation) -> {
                        final Comparator<Value> comparator = extractComparatorFromParameters(context, values, localInformation);

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

                    put("join", (context, self, values, localInformation) -> {
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

                    put("iterator", (context, self, values, localInformation) -> makeIteratorValueIterator(((Map<Object, Value>) self.getValue()).entrySet().iterator()));

                    put("sum", (context, self, values, localInformation) -> new Value(((Map<Object, Value>) self.getValue()).values().stream().map(Value::getNumericValue).reduce((a, b) -> a.add(b)).orElse(new BigDecimal(0))));
                    put("avg", (context, self, values, localInformation) -> new Value(((Map<Object, Value>) self.getValue()).values().stream().map(Value::getNumericValue).reduce((a, b) -> a.add(b)).orElse(new BigDecimal(0)).divide(new BigDecimal(((Map<Object, Value>) self.getValue()).size()), RoundingMode.HALF_UP)));
                    put("max", (context, self, values, localInformation) -> {
                        final Comparator<Value> comparator = extractComparatorFromParameters(context, values, localInformation);
                        return new Value(((Map<Object, Value>) self.getValue()).values().stream().max(comparator).orElse(Value.empty()));
                    });
                    put("min", (context, self, values, localInformation) -> {
                        final Comparator<Value> comparator = extractComparatorFromParameters(context, values, localInformation);
                        return new Value(((Map<Object, Value>) self.getValue()).values().stream().min(comparator).orElse(Value.empty()));
                    });
                }
            });
            put(PrimitiveValueType.STRING.getType(), new HashMap<String, MenterValueFunction>() {
                {
                    put("size", (context, self, values, localInformation) -> new Value(self.size()));
                    put("charAt", (context, self, values, localInformation) -> new Value(((String) self.getValue()).charAt(values.get(0).getNumericValue().intValue())));

                    put("iterator", (context, self, values, localInformation) -> makeIteratorValueIterator(((String) self.getValue()).chars().mapToObj(c -> (char) c).iterator()));
                }
            });
            put(PrimitiveValueType.ITERATOR.getType(), new HashMap<String, MenterValueFunction>() {
                {
                    put("hasNext", (context, self, values, localInformation) -> new Value(((Iterator<?>) self.getValue()).hasNext()));
                    put("next", (context, self, values, localInformation) -> new Value(((Iterator<?>) self.getValue()).next()));

                    put("forEach", (context, self, values, localInformation) -> {
                        final Iterator<?> iterator = (Iterator<?>) self.getValue();
                        while (iterator.hasNext()) {
                            applyFunction(toList(new Value(iterator.next())), values.get(0), context, localInformation, "forEach");
                        }
                        return self;
                    });

                    put("iterator", (context, self, values, localInformation) -> self);
                }
            });
            put(PrimitiveValueType.ANY.getType(), new HashMap<String, MenterValueFunction>() {
                {
                    put("type", (context, self, values, localInformation) -> new Value(self.getType()));

                    put("forEach", (context, self, values, localInformation) -> {
                        final Iterator<?> iterator = (Iterator<?>) applyFunction(toList(self), self.access(new Value("iterator")), context, localInformation, "iterator").getValue();
                        while (iterator.hasNext()) {
                            applyFunction(toList(new Value(iterator.next())), values.get(0), context, localInformation, "forEach");
                        }
                        return self;
                    });

                    put("functions", (context, self, values, localInformation) -> {
                        final List<Value> result = new ArrayList<>();
                        VALUE_FUNCTIONS.getOrDefault(self.getType(), new HashMap<>()).keySet().forEach(f -> result.add(new Value(f)));
                        return new Value(result);
                    });

                    put("isNull", (context, self, values, localInformation) -> new Value(self.isEmpty()));
                }
            });
        }
    };

    public static Comparator<Value> extractComparatorFromParameters(GlobalContext context, List<Value> values, EvaluationContextLocalInformation localInformation) {
        final Comparator<Value> comparator;
        if (values.size() > 0) {
            final List<String> parameterList = getParameterList(values.get(0));
            if (parameterList == null || parameterList.size() == 2) {
                comparator = (a, b) -> {
                    final Value result = applyFunction(toList(a, b), values.get(0), context, localInformation, "compareTo");
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
        return comparator;
    }

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

    private static Value applyFunction(List<Value> value, Value function, GlobalContext context, EvaluationContextLocalInformation localInformation, String originalFunctionName) {
        return context.evaluateFunction(function, value, context, localInformation, originalFunctionName);
    }

    private static List<String> getParameterList(Value value) {
        if (!value.isFunction()) {
            throw new MenterExecutionException("Value is not a function " + value);
        }

        if (value.getValue() instanceof MenterNodeFunction) {
            final MenterNodeFunction executableFunction = (MenterNodeFunction) value.getValue();
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
        return toDisplayString(value) + " (" + getType() + ")";
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
        } else if (object instanceof MenterNodeFunction) {
            return String.valueOf(object);

        } else if (object instanceof Iterator) {
            // cannot print the iterator, because it will consume the values
            return "<<iterator>>";

        } else {
            for (CustomValueType type : CUSTOM_VALUE_TYPES) {
                if (type.isType(object)) {
                    return type.toDisplayString(object);
                }
            }

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
            return this.getNumericValue().compareTo(other.getNumericValue()) == 0;
        } else if (this.getType().equals(PrimitiveValueType.BOOLEAN.getType())) {
            return this.isTrue() == other.isTrue();
        }

        return this.getValue().equals(other.getValue());
    }

    @Override
    public int compareTo(Value o) {
        if (this.getType().equals(PrimitiveValueType.NUMBER.getType())) {
            return this.getNumericValue().compareTo(o.getNumericValue());
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
