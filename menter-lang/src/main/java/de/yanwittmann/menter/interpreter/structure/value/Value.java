package de.yanwittmann.menter.interpreter.structure.value;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.structure.Module;
import de.yanwittmann.menter.interpreter.structure.*;
import de.yanwittmann.menter.lexer.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Value implements Comparable<Value> {

    private final static Logger LOG = LogManager.getLogger(Value.class);
    public final static String TAG_KEY_FUNCTION_PARENT_VALUE = "functionParentValue";
    public final static String TAG_KEY_FUNCTION_CLOSURE_CONTEXT = "functionParentClosureContext";
    public final static String TAG_KEY_FUNCTION_CLOSURE_LOCAL_INFORMATION = "functionParentClosureLocalInformation";
    public final static String TAG_KEY_RETURN_VALUE = "returnValue";
    public final static String TAG_KEY_BREAK_VALUE = "breakValue";
    public final static String TAG_KEY_CONTINUE_VALUE = "continueValue";

    private final static List<Module> CUSTOM_TYPES = new ArrayList<>();

    private final int uuidHash = UUID.randomUUID().toString().hashCode();
    private Object value;
    private Map.Entry<String, Value>[] taggedAdditionalInformation = new Map.Entry[0];

    public Value(Object value) {
        setValue(value);
    }

    public Value(Object value, String taggedInformationKey, Value taggedInformationValue) {
        this(value);
        setTaggedAdditionalInformation(taggedInformationKey, taggedInformationValue);
    }

    public Object getValue() {
        return value;
    }

    public LinkedHashMap<Object, Value> getMap() {
        if (value instanceof LinkedHashMap) return (LinkedHashMap<Object, Value>) value;
        else throw new MenterExecutionException("Cannot transform type " + getType() + " to map");
    }

    public void setTaggedAdditionalInformation(String key, Value value) {
        if (key != null && value != null) {
            removeTaggedAdditionalInformation(key);
            final Map.Entry<String, Value>[] taggedInfo = new Map.Entry[taggedAdditionalInformation.length + 1];
            System.arraycopy(taggedAdditionalInformation, 0, taggedInfo, 0, taggedAdditionalInformation.length);
            taggedInfo[taggedAdditionalInformation.length] = new AbstractMap.SimpleEntry<>(key, value);
            taggedAdditionalInformation = taggedInfo;
        }
    }

    private Value getTaggedAdditionalInformation(String key) {
        for (Map.Entry<String, Value> entry : taggedAdditionalInformation) {
            if (entry.getKey().equals(key)) return entry.getValue();
        }
        return null;
    }

    public void clearTaggedAdditionalInformation() {
        taggedAdditionalInformation = new Map.Entry[0];
    }

    public void removeTaggedAdditionalInformation(String key) {
        final List<Map.Entry<String, Value>> taggedInfo = new ArrayList<>(Arrays.asList(taggedAdditionalInformation));
        taggedInfo.removeIf(entry -> entry.getKey().equals(key));
        taggedAdditionalInformation = taggedInfo.toArray(new Map.Entry[0]);
    }

    public boolean hasTaggedAdditionalInformation(String key) {
        return getTaggedAdditionalInformation(key) != null;
    }

    public Value getTagParentFunctionValue() {
        return getTaggedAdditionalInformation(TAG_KEY_FUNCTION_PARENT_VALUE);
    }

    public boolean isReturn() {
        return hasTaggedAdditionalInformation(Value.TAG_KEY_RETURN_VALUE);
    }

    public boolean isBreak() {
        return hasTaggedAdditionalInformation(Value.TAG_KEY_BREAK_VALUE);
    }

    public boolean isContinue() {
        return hasTaggedAdditionalInformation(Value.TAG_KEY_CONTINUE_VALUE);
    }

    public void setReturn(boolean isReturn) {
        if (isReturn) setTaggedAdditionalInformation(Value.TAG_KEY_RETURN_VALUE, new Value(true));
        else removeTaggedAdditionalInformation(Value.TAG_KEY_RETURN_VALUE);
    }

    public void setBreak(boolean isBreak) {
        if (isBreak) setTaggedAdditionalInformation(Value.TAG_KEY_BREAK_VALUE, new Value(true));
        else removeTaggedAdditionalInformation(Value.TAG_KEY_BREAK_VALUE);
    }

    public void setContinue(boolean isContinue) {
        if (isContinue) setTaggedAdditionalInformation(Value.TAG_KEY_CONTINUE_VALUE, new Value(true));
        else removeTaggedAdditionalInformation(Value.TAG_KEY_CONTINUE_VALUE);
    }

    public boolean unwrapReturn() {
        if (isReturn()) {
            removeTaggedAdditionalInformation(Value.TAG_KEY_RETURN_VALUE);
            return true;
        } else if (isBreak()) {
            removeTaggedAdditionalInformation(Value.TAG_KEY_BREAK_VALUE);
        } else if (isContinue()) {
            removeTaggedAdditionalInformation(Value.TAG_KEY_CONTINUE_VALUE);
        }
        return false;
    }

    public boolean unwrapBreak() {
        if (isBreak()) {
            removeTaggedAdditionalInformation(Value.TAG_KEY_BREAK_VALUE);
            return true;
        } else if (isReturn()) {
            return true;
        } else if (isContinue()) {
            removeTaggedAdditionalInformation(Value.TAG_KEY_CONTINUE_VALUE);
        }
        return false;
    }

    public boolean unwrapContinue() {
        if (isContinue()) {
            removeTaggedAdditionalInformation(Value.TAG_KEY_CONTINUE_VALUE);
            return true;
        } else if (isReturn()) {
            removeTaggedAdditionalInformation(Value.TAG_KEY_RETURN_VALUE);
        } else if (isBreak()) {
            removeTaggedAdditionalInformation(Value.TAG_KEY_BREAK_VALUE);
        }
        return false;
    }

    public EvaluationContextLocalInformation getTagParentFunctionClosureLocalInformation() {
        final Value element = getTaggedAdditionalInformation(TAG_KEY_FUNCTION_CLOSURE_LOCAL_INFORMATION);
        if (element != null) return (EvaluationContextLocalInformation) element.getValue();
        else return null;
    }

    public void setTagParentFunctionClosureLocalInformation(EvaluationContextLocalInformation localInformation) {
        setTaggedAdditionalInformation(TAG_KEY_FUNCTION_CLOSURE_LOCAL_INFORMATION, new Value(localInformation));
    }

    public GlobalContext getTagParentFunctionClosureContext() {
        final Value element = getTaggedAdditionalInformation(TAG_KEY_FUNCTION_CLOSURE_CONTEXT);
        if (element != null) return (GlobalContext) element.getValue();
        else return null;
    }

    public void setTagParentFunctionClosureContext(GlobalContext context) {
        setTaggedAdditionalInformation(TAG_KEY_FUNCTION_CLOSURE_CONTEXT, new Value(context));
    }

    public void inheritValue(Value value) {
        if (value == null) {
            throw new MenterExecutionException("Cannot inherit value from null");
        }
        this.value = value.value;
        clearTaggedAdditionalInformation();
        for (Map.Entry<String, Value> entry : value.taggedAdditionalInformation) {
            setTaggedAdditionalInformation(entry.getKey(), entry.getValue());
        }
    }

    public void setValue(Object value) {
        if (value instanceof Integer) this.value = new BigDecimal("" + value);
        else if (value instanceof Long) this.value = new BigDecimal("" + value);
        else if (value instanceof Float) this.value = new BigDecimal("" + value);
        else if (value instanceof Double) this.value = new BigDecimal("" + value);
        else if (value instanceof Character) this.value = String.valueOf(value);
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
            setValue(((Value) value).getValue());
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
        } else if (value instanceof NativeFunction) {
            return PrimitiveValueType.NATIVE_FUNCTION.getType();
        } else if (value instanceof Method) {
            return PrimitiveValueType.REFLECTIVE_FUNCTION.getType();
        } else if (value instanceof Iterator<?>) {
            return PrimitiveValueType.ITERATOR.getType();
        } else if (value instanceof CustomType) {
            return PrimitiveValueType.CUSTOM_TYPE.getType();
        } else {
            if (this.getValue() instanceof CustomType) {
                return this.getValue().toString();
            }

            return value.getClass().getSimpleName();
        }
    }

    public boolean isFunction() {
        final String type = getType();
        return type.equals(PrimitiveValueType.FUNCTION.getType()) ||
               type.equals(PrimitiveValueType.VALUE_FUNCTION.getType()) ||
               type.equals(PrimitiveValueType.NATIVE_FUNCTION.getType()) ||
               type.equals(PrimitiveValueType.REFLECTIVE_FUNCTION.getType());
    }

    public BigDecimal getNumericValue() {
        if (Objects.equals(this.getType(), PrimitiveValueType.NUMBER.getType())) {
            return (BigDecimal) value;
        } else {
            if (this.getValue() instanceof CustomType) {
                return ((CustomType) this.getValue()).getNumericValue();
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

        if (this.getValue() instanceof CustomType) {
            return ((CustomType) this.getValue()).isTrue();
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
            if (this.getValue() instanceof CustomType) {
                return ((CustomType) this.getValue()).size();
            }

            return 1;
        }
    }

    public Value iterator() {
        if (value instanceof Map) {
            return makeIteratorValueIterator(((Map<Object, Value>) this.getValue()).entrySet().iterator());

        } else if (value instanceof String) {
            return makeIteratorValueIterator(((String) this.getValue()).chars().mapToObj(c -> (char) c).iterator());

        } else if (value instanceof CustomType) {
            final Value iterator = ((CustomType) value).iterator();
            if (iterator != null) {
                return iterator;
            }
        }

        throw new MenterExecutionException("[" + getType() + "] is not iterable.");
    }

    public static Value makeIteratorValueIterator(Iterator<?> iterator) {
        return new Value(new Iterator<Value>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Value next() {
                final Object next = iterator.next();
                if (next instanceof Map.Entry) {
                    return new Value(Arrays.asList(
                            new Value(((Map.Entry<?, ?>) next).getKey()),
                            new Value(((Map.Entry<?, ?>) next).getValue())
                    ));
                } else {
                    return new Value(next);
                }
            }
        });
    }

    public Value access(Value identifier) {
        if (identifier.getValue() == null) {
            return Value.empty();
        }

        if (VALUE_FUNCTIONS.containsKey(this.getType()) && VALUE_FUNCTIONS.get(this.getType()).containsKey(String.valueOf(identifier.getValue()))) {
            return new Value(VALUE_FUNCTIONS.get(this.getType()).get(identifier.getValue().toString()), TAG_KEY_FUNCTION_PARENT_VALUE, this);
        }
        if (VALUE_FUNCTIONS.get(PrimitiveValueType.ANY.getType()).containsKey(String.valueOf(identifier.getValue()))) {
            return new Value(VALUE_FUNCTIONS.get(PrimitiveValueType.ANY.getType()).get(identifier.getValue().toString()), TAG_KEY_FUNCTION_PARENT_VALUE, this);
        }

        if (this.getType().equals(PrimitiveValueType.OBJECT.getType()) || this.getType().equals(PrimitiveValueType.ARRAY.getType())) {
            final Object accessValue = identifier.getValue();
            final Map<Object, Value> map = (Map<Object, Value>) value;

            if (accessValue instanceof BigDecimal) {
                if (map.containsKey(accessValue)) {
                    return map.get(accessValue);
                } else {
                    // check every key using the compareTo method. This is sadly not preventable, as BigDecimal
                    // sometimes uses the scientific notation to compare values, and not only the plain version.
                    // or, at least I have not found a different way to do this.
                    for (Object key : map.keySet()) {
                        if (key instanceof BigDecimal) {
                            if (((BigDecimal) key).compareTo((BigDecimal) accessValue) == 0) {
                                return map.get(key);
                            }
                        }
                    }
                }
            } else {
                return map.get(accessValue);
            }
        }

        if (this.getValue() instanceof CustomType) {
            final CustomType customType = (CustomType) this.getValue();
            final Value accessed = customType.accessValue(identifier);
            if (accessed != null) {
                return accessed;
            }
        }
        if (this.getValue() instanceof Class<?> && CustomType.class.isAssignableFrom(((Class<?>) this.getValue()))) {
            final Class<CustomType> customTypeClass = (Class<CustomType>) this.getValue();
            return CustomType.accessStaticValue(customTypeClass, identifier);
        }

        return null;
    }

    public boolean create(Value identifier, Value value, boolean isFinalIdentifier) {
        if (this.getType().equals(PrimitiveValueType.OBJECT.getType()) || this.getType().equals(PrimitiveValueType.ARRAY.getType())) {
            if (!isFinalIdentifier) {
                value.setValue(new LinkedHashMap<>());
            }
            ((Map<Object, Value>) this.value).put(identifier.getValue(), value);
            return true;
        }

        return false;
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

    public static void registerCustomValueType(Class<? extends CustomType> type) {
        final String moduleName = CustomType.getModuleName(type);
        final String typeName = CustomType.getTypeName(type);

        final Module existingModule = CUSTOM_TYPES.stream()
                .filter(m -> m.getName().equals(moduleName))
                .findFirst()
                .orElse(null);

        final Module effectiveModule = existingModule == null
                ? new Module(new GlobalContext("custom_module_" + moduleName), moduleName)
                : existingModule;
        effectiveModule.addSymbol(typeName);

        if (existingModule == null) {
            CUSTOM_TYPES.add(effectiveModule);
        }

        effectiveModule.getParentContext().addVariable(typeName, new Value(type));

        LOG.info("Registered custom type [{}.{}]", moduleName, typeName);
    }

    public static void unregisterCustomValueType(Class<? extends CustomType> type) {
        for (int i = CUSTOM_TYPES.size() - 1; i >= 0; i--) {
            final Module module = CUSTOM_TYPES.get(i);

            final String variableName = module.getParentContext()
                    .getVariables()
                    .entrySet().stream()
                    .filter(e -> e.getValue().getValue() == type)
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (variableName != null) {
                module.getParentContext().removeVariable(variableName);
                if (module.getParentContext().getVariables().size() == 0) {
                    CUSTOM_TYPES.remove(module);
                }
                break;
            }
        }
    }

    public static Module findCustomTypeModule(String name) {
        return CUSTOM_TYPES.stream()
                .filter(module -> module.getName().equals(name))
                .findFirst()
                .orElse(null);
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

                    put("push", (context, self, values, localInformation) -> {
                        // either one or two arguments, if only one argument is given, the key is the size of the map
                        if (values.size() == 1) {
                            final BigDecimal max = findHighestNumericKey((Map<Object, Value>) self.getValue());
                            ((Map<Object, Value>) self.getValue()).put(max.add(BigDecimal.ONE), values.get(0));
                        } else {
                            ((Map<Object, Value>) self.getValue()).put(values.get(0).getValue(), values.get(1));
                        }
                        return self;
                    });
                    put("pop", (context, self, values, localInformation) -> {
                        final Map<Object, Value> map = (Map<Object, Value>) self.getValue();
                        if (map.size() > 0) {
                            if (isMapAnArray(map)) {
                                final BigDecimal max = findHighestNumericKey(map);
                                return (map).remove(max);
                            } else {
                                return (map).remove(values.get(0).getValue());
                            }
                        } else {
                            return Value.empty();
                        }
                    });

                    put("map", (context, self, values, localInformation) -> {
                        final Map<Object, Value> map = new LinkedHashMap<>();

                        if (values.get(0).getValue() instanceof MenterNodeFunction && ((MenterNodeFunction) values.get(0).getValue()).getArgumentNames().size() == 2) {
                            for (Entry<Object, Value> entry : (self.getMap()).entrySet()) {
                                map.put(entry.getKey(), applyFunction(toList(entry.getKey(), entry.getValue()), values.get(0), context, localInformation, "map"));
                            }
                        } else {
                            for (Entry<Object, Value> entry : (self.getMap()).entrySet()) {
                                map.put(entry.getKey(), applyFunction(toList(entry.getValue()), values.get(0), context, localInformation, "map"));
                            }
                        }

                        return new Value(map);
                    });
                    put("mapKeys", (context, self, values, localInformation) -> {
                        final Map<Object, Value> map = new LinkedHashMap<>();
                        for (Entry<Object, Value> entry : (self.getMap()).entrySet()) {
                            map.put(applyFunction(toList(entry.getKey()), values.get(0), context, localInformation, "mapKeys").getValue(), entry.getValue());
                        }
                        return new Value(map);
                    });

                    put("filter", (context, self, values, localInformation) -> {
                        if (isMapAnArray((self.getMap()))) {
                            final List<Value> mapped = new ArrayList<>();
                            for (Entry<Object, Value> entry : (self.getMap()).entrySet()) {
                                if (applyFunction(toList(entry.getValue()), values.get(0), context, localInformation, "filter").isTrue()) {
                                    mapped.add(entry.getValue());
                                }
                            }
                            return new Value(mapped);

                        } else {
                            final Map<Object, Value> map = new LinkedHashMap<>();
                            for (Entry<Object, Value> entry : (self.getMap()).entrySet()) {
                                if (applyFunction(toList(entry.getValue()), values.get(0), context, localInformation, "filter").isTrue()) {
                                    map.put(entry.getKey(), entry.getValue());
                                }
                            }
                            return new Value(map);
                        }
                    });
                    put("filterKeys", (context, self, values, localInformation) -> {
                        final Map<Object, Value> map = new LinkedHashMap<>();
                        for (Entry<Object, Value> entry : (self.getMap()).entrySet()) {
                            if (applyFunction(toList(entry.getKey()), values.get(0), context, localInformation, "filterKeys").isTrue()) {
                                map.put(entry.getKey(), entry.getValue());
                            }
                        }
                        return new Value(map);
                    });

                    put("distinct", (context, self, values, localInformation) -> {
                        if (isMapAnArray((self.getMap()))) {
                            final List<Value> mapped = new ArrayList<>();
                            for (Entry<Object, Value> entry : (self.getMap()).entrySet()) {
                                if (!mapped.contains(entry.getValue())) {
                                    mapped.add(entry.getValue());
                                }
                            }
                            return new Value(mapped);

                        } else {
                            final Map<Object, Value> map = new LinkedHashMap<>();
                            for (Entry<Object, Value> entry : (self.getMap()).entrySet()) {
                                if (!map.containsValue(entry.getValue())) {
                                    map.put(entry.getKey(), entry.getValue());
                                }
                            }
                            return new Value(map);
                        }
                    });

                    put("sort", (context, self, values, localInformation) -> {
                        final Comparator<Value> comparator = extractComparatorFromParameters(context, values, localInformation);

                        if (isMapAnArray((self.getMap()))) {
                            return new Value((self.getMap()).values().stream()
                                    .sorted(comparator)
                                    .collect(Collectors.toList()));
                        } else {
                            return new Value((self.getMap()).entrySet().stream()
                                    .sorted((a, b) -> comparator.compare(a.getValue(), b.getValue()))
                                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
                        }
                    });

                    put("sortKey", (context, self, values, localInformation) -> {
                        final Comparator<Value> comparator = extractComparatorFromParameters(context, values, localInformation);

                        if (isMapAnArray((self.getMap()))) {
                            return new Value((self.getMap()).values().stream()
                                    .sorted(comparator)
                                    .collect(Collectors.toList()));
                        } else {
                            return new Value((self.getMap()).entrySet().stream()
                                    .sorted((a, b) -> comparator.compare(new Value(a.getKey()), new Value(b.getKey())))
                                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
                        }
                    });

                    put("join", (context, self, values, localInformation) -> {
                        final String separator = values.size() > 0 ? values.get(0).toDisplayString() : "";
                        final String prefix = values.size() > 1 ? values.get(1).toDisplayString() : "";
                        final String suffix = values.size() > 2 ? values.get(2).toDisplayString() : "";
                        final StringBuilder sb = new StringBuilder();
                        sb.append(prefix);
                        for (Entry<Object, Value> entry : (self.getMap()).entrySet()) {
                            if (sb.length() > prefix.length()) {
                                sb.append(separator);
                            }
                            sb.append(entry.getValue().toDisplayString());
                        }
                        sb.append(suffix);
                        return new Value(sb.toString());
                    });

                    put("reduce", (context, self, values, localInformation) -> Value.fold(context, self, values, localInformation, true));
                    put("foldl", (context, self, values, localInformation) -> Value.fold(context, self, values, localInformation, true));
                    put("foldr", (context, self, values, localInformation) -> Value.fold(context, self, values, localInformation, false));

                    put("sum", (context, self, values, localInformation) -> new Value((self.getMap()).values().stream().map(Value::getNumericValue).reduce(BigDecimal::add).orElse(new BigDecimal(0))));
                    put("avg", (context, self, values, localInformation) -> new Value((self.getMap()).values().stream().map(Value::getNumericValue).reduce(BigDecimal::add).orElse(new BigDecimal(0)).divide(new BigDecimal((self.getMap()).size()), RoundingMode.HALF_UP)));
                    put("max", (context, self, values, localInformation) -> {
                        final Comparator<Value> comparator = extractComparatorFromParameters(context, values, localInformation);
                        return new Value((self.getMap()).values().stream().max(comparator).orElse(Value.empty()));
                    });
                    put("min", (context, self, values, localInformation) -> {
                        final Comparator<Value> comparator = extractComparatorFromParameters(context, values, localInformation);
                        return new Value((self.getMap()).values().stream().min(comparator).orElse(Value.empty()));
                    });

                    put("head", (context, self, values, localInformation) -> {
                        if (isMapAnArray((self.getMap()))) {
                            return (self.getMap()).values().stream().findFirst().orElse(Value.empty());
                        } else {
                            return (self.getMap()).entrySet().stream().findFirst().map(Entry::getValue).orElse(Value.empty());
                        }
                    });
                    put("tail", (context, self, values, localInformation) -> {
                        if (isMapAnArray(self.getMap())) {
                            return new Value((self.getMap()).values().stream().skip(1).collect(Collectors.toList()));
                        } else {
                            return new Value((self.getMap()).entrySet().stream().skip(1).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
                        }
                    });

                    put("cross", (context, self, values, localInformation) -> {
                        // generate all combinations of the two maps (self, parameter)
                        // might apply the filter function in the second parameter
                        final List<Value> result = new ArrayList<>();
                        final Value filter = values.size() > 1 ? values.get(1) : null;

                        for (Entry<Object, Value> next1 : (self.getMap()).entrySet()) {
                            for (Entry<Object, Value> next2 : (values.get(0).getMap()).entrySet()) {
                                if (filter == null || context.evaluateFunction("filter", filter, context, localInformation, next1.getValue(), next2.getValue()).isTrue()) {
                                    final Map<Object, Value> map = new LinkedHashMap<>();
                                    if (!(next1.getValue().getValue() instanceof Map) && !(next2.getValue().getValue() instanceof Map)) {
                                        map.put(0, next1.getValue());
                                        map.put(1, next2.getValue());
                                    } else if (!(next1.getValue().getValue() instanceof Map)) {
                                        map.putAll(next2.getValue().getMap());
                                        BigDecimal max = findHighestNumericKey(map);
                                        map.put(max.add(BigDecimal.ONE), next1.getValue());
                                    } else if (!(next2.getValue().getValue() instanceof Map)) {
                                        map.putAll(next1.getValue().getMap());
                                        BigDecimal max = findHighestNumericKey(map);
                                        map.put(max.add(BigDecimal.ONE), next2.getValue());
                                    } else {
                                        map.putAll(next1.getValue().getMap());
                                        map.putAll(next2.getValue().getMap());
                                    }
                                    result.add(new Value(map));
                                }
                            }
                        }
                        return new Value(result);
                    });

                    put("frequency", (context, self, values, localInformation) -> {
                        // count the number of occurrences of each value in the map
                        final Map<Object, Integer> frequency = new HashMap<>();
                        for (Value value : (self.getMap()).values()) {
                            frequency.put(value.getValue(), frequency.getOrDefault(value.getValue(), 0) + 1);
                        }
                        final Map<Value, Value> result = new HashMap<>();
                        for (Entry<Object, Integer> entry : frequency.entrySet()) {
                            result.put(new Value(entry.getKey()), new Value(entry.getValue()));
                        }
                        return new Value(result);
                    });

                    put("rename", (context, self, values, localInformation) -> {
                        // modifies the source map by renaming the key (first parameter) to the second parameter
                        final Map<Object, Value> map = self.getMap();
                        final Object key = values.get(0).getValue();
                        final Object newKey = values.get(1).getValue();

                        if (map.containsKey(key)) {
                            final Value value = map.get(key);
                            map.remove(key);
                            map.put(newKey, value);
                        }
                        return self;
                    });

                    put("removeKey", (context, self, values, localInformation) -> {
                        // first parameter might be a value, might also be a function
                        // apply modifications directly to the source map
                        final Value value = values.get(0);
                        if (value.isFunction()) {
                            final Map<Object, Value> map = self.getMap();
                            final List<Object> keysToRemove = new ArrayList<>();
                            for (Entry<Object, Value> entry : map.entrySet()) {
                                if (context.evaluateFunction("filter", value, context, localInformation, new Value(entry.getKey())).isTrue()) {
                                    keysToRemove.add(entry.getKey());
                                }
                            }
                            for (Object key : keysToRemove) {
                                map.remove(key);
                            }
                        } else {
                            self.getMap().remove(value.getValue());
                        }
                        return self;
                    });

                    put("retainKey", (context, self, values, localInformation) -> {
                        // first parameter might be a value, might also be a function
                        // apply modifications directly to the source map
                        final Value value = values.get(0);
                        if (value.isFunction()) {
                            final Map<Object, Value> map = self.getMap();
                            final List<Object> keysToRemove = new ArrayList<>();
                            for (Entry<Object, Value> entry : map.entrySet()) {
                                if (!context.evaluateFunction("filter", value, context, localInformation, new Value(entry.getKey())).isTrue()) {
                                    keysToRemove.add(entry.getKey());
                                }
                            }
                            for (Object key : keysToRemove) {
                                map.remove(key);
                            }
                        } else {
                            final Map<Object, Value> map = self.getMap();
                            final List<Object> keysToRemove = new ArrayList<>();
                            for (Entry<Object, Value> entry : map.entrySet()) {
                                if (!entry.getKey().equals(value.getValue())) {
                                    keysToRemove.add(entry.getKey());
                                }
                            }
                            for (Object key : keysToRemove) {
                                map.remove(key);
                            }
                        }
                        return self;
                    });
                }
            });
            put(PrimitiveValueType.STRING.getType(), new HashMap<String, MenterValueFunction>() {
                {
                    put("size", (context, self, values, localInformation) -> new Value(self.size()));
                    put("charAt", (context, self, values, localInformation) -> new Value(((String) self.getValue()).charAt(values.get(0).getNumericValue().intValue())));
                }
            });
            put(PrimitiveValueType.ITERATOR.getType(), new HashMap<String, MenterValueFunction>() {
                {
                    put("iterator", (context, self, values, localInformation) -> self);
                    put("hasNext", (context, self, values, localInformation) -> new Value(((Iterator<?>) self.getValue()).hasNext()));
                    put("next", (context, self, values, localInformation) -> new Value(((Iterator<?>) self.getValue()).next()));
                }
            });
            put(PrimitiveValueType.ANY.getType(), new HashMap<String, MenterValueFunction>() {
                {
                    put("type", (context, self, values, localInformation) -> new Value(self.getType()));

                    put("iterator", (context, self, values, localInformation) -> self.iterator());
                    put("forEach", (context, self, values, localInformation) -> context.forLoop(self, values.get(0), context, localInformation));

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

    private static BigDecimal findHighestNumericKey(Map<Object, Value> map) {
        BigDecimal max = BigDecimal.valueOf(-1);
        for (Object key : map.keySet()) {
            if (key instanceof BigDecimal) {
                max = max.max((BigDecimal) key);
            }
        }
        return max;
    }

    private static Value fold(GlobalContext context, Value self, List<Value> values, EvaluationContextLocalInformation localInformation, boolean left) {
        final List<Value> list = new ArrayList<>((self.getMap()).values());
        if (list.isEmpty()) {
            return Value.empty();
        }

        Value aggregator = null;
        if (values.size() == 2) {
            aggregator = values.get(0);
        }
        final Value function = values.get(values.size() - 1);

        if (left) {
            if (aggregator == null) {
                aggregator = list.get(0);
            }
            for (int i = 1; i < list.size(); i++) {
                aggregator = applyFunction(toList(aggregator, list.get(i)), function, context, localInformation, "foldl");
            }
        } else {
            if (aggregator == null) {
                aggregator = list.get(list.size() - 1);
            }
            for (int i = list.size() - 2; i >= 0; i--) {
                aggregator = applyFunction(toList(aggregator, list.get(i)), function, context, localInformation, "foldr");
            }
        }
        return aggregator;
    }

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
        return toDisplayStringInternal(object, new HashSet<>());
    }

    private static String toDisplayStringInternal(Object object, Set<Object> visited) {
        if (object == null) {
            return "null";
        } else if (visited.contains(object)) {
            return "<circular-reference-" +
                   (object instanceof Value ? ((Value) object).getType() : object.getClass().getSimpleName()) + "@" + Objects.hashCode(object)
                   + ">";
        } else {
            boolean add = true;
            if (object instanceof String || object instanceof BigDecimal || object instanceof Pattern || object instanceof Boolean || object instanceof Iterator) {
                add = false;
            } else if (object instanceof Value) {
                if ((PrimitiveValueType.isType(((Value) object), PrimitiveValueType.NUMBER.getType()) || PrimitiveValueType.isType(((Value) object), PrimitiveValueType.STRING.getType()) ||
                     PrimitiveValueType.isType(((Value) object), PrimitiveValueType.BOOLEAN.getType()) || PrimitiveValueType.isType(((Value) object), PrimitiveValueType.REGEX.getType()))) {
                    add = false;
                }
            }
            if (add) {
                visited.add(object);
            }
        }

        try {
            if (object instanceof Value) {
                return toDisplayStringInternal(((Value) object).getValue(), visited);

            } else if (object instanceof List) {
                return ((List<?>) object).stream()
                        .map(v -> toDisplayStringInternal(v, visited))
                        .collect(Collectors.joining(", ", "[", "]"));

            } else if (object instanceof Map) {
                final Map<?, ?> map = (Map<?, ?>) object;
                if (isMapAnArray(map)) {
                    return toDisplayStringInternal(new ArrayList<>(map.values()), visited);
                } else {
                    final StringJoiner joiner = new StringJoiner(", ", "{", "}");
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        joiner.add(toDisplayStringInternal(entry.getKey(), visited) + ": " + toDisplayStringInternal(entry.getValue(), visited));
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

            } else if (object instanceof Method) {
                final Method method = (Method) object;
                return method.getDeclaringClass().getSimpleName() + "." + method.getName();

            } else if (object.getClass().getSimpleName().contains("Lambda$")) {
                return "<<lambda>>";

            } else {
                // custom type overrides the toString method, which is why not separate call is needed

                return String.valueOf(object);
            }
        } finally {
            visited.remove(object);
        }
    }

    public static boolean isMapAnArray(Map<?, ?> map) {
        if (map.isEmpty()) {
            return true;
        }

        if (!map.keySet().stream().allMatch(k -> k instanceof BigDecimal)) {
            return false;
        }

        if (!map.containsKey(BigDecimal.ZERO)) {
            return false;
        }

        return map.keySet().stream().map(k -> (BigDecimal) k).max(BigDecimal::compareTo).get().intValue() == map.size() - 1;
    }

    public static boolean isMapAnArray(Value value) {
        return value.getValue() instanceof Map && isMapAnArray((Map<?, ?>) value.getValue());
    }

    public boolean isMapAnArray() {
        return isMapAnArray(this);
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
            return this.toDisplayString().equals(other.toDisplayString());
        } else if (this.getType().equals(PrimitiveValueType.BOOLEAN.getType())) {
            return this.isTrue() == other.isTrue();
        }

        return this.getValue().equals(other.getValue());
    }

    @Override
    public int compareTo(Value o) {
        final String type = this.getType();
        if (type.equals(PrimitiveValueType.NUMBER.getType())) {
            return this.getNumericValue().compareTo(o.getNumericValue());
        } else if (type.equals(PrimitiveValueType.BOOLEAN.getType())) {
            return Boolean.compare(this.isTrue(), o.isTrue());
        } else if (type.equals(PrimitiveValueType.STRING.getType())) {
            return this.getValue().toString().compareTo(o.getValue().toString());
        } else if (type.equals(PrimitiveValueType.ARRAY.getType())) {
            return Integer.compare(((Map<?, ?>) this.getValue()).size(), ((Map<?, ?>) o.getValue()).size());
        } else if (type.equals(PrimitiveValueType.OBJECT.getType())) {
            return Integer.compare(((Map<?, ?>) this.getValue()).size(), ((Map<?, ?>) o.getValue()).size());
        } else if (type.equals(PrimitiveValueType.CUSTOM_TYPE.getType())) {
            return ((CustomType) this.getValue()).compareTo(((CustomType) o.getValue()));
        } else {
            return Integer.compare(this.getValue().hashCode(), o.getValue().hashCode());
        }
    }

    @Override
    public int hashCode() {
        return uuidHash;
    }
}
