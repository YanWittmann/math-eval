package de.yanwittmann.menter.interpreter.structure.value;

import de.yanwittmann.menter.exceptions.MenterExecutionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class CustomType implements Comparable<CustomType> {

    public CustomType(List<Value> parameters) {
    }

    public String getTypeName() {
        return this.getClass().getAnnotation(TypeMetaData.class).typeName();
    }

    public String getModuleName() {
        return this.getClass().getAnnotation(TypeMetaData.class).moduleName();
    }

    public static String getTypeName(Class<? extends CustomType> type) {
        return type.getAnnotation(TypeMetaData.class).typeName();
    }

    public static String getModuleName(Class<? extends CustomType> type) {
        return type.getAnnotation(TypeMetaData.class).moduleName();
    }

    public Value accessValue(Value identifier) {
        return new Value(findReflectiveMethod(identifier.toDisplayString()), Value.TAG_KEY_FUNCTION_PARENT_VALUE, new Value(this));
    }

    public static Value accessStaticValue(Class<CustomType> clazz, Value identifier) {
        return new Value(findReflectiveMethod(clazz, identifier.toDisplayString()), Value.TAG_KEY_FUNCTION_PARENT_VALUE, new Value(null));
    }

    public boolean createAccessedValue(Value identifier, Value accessedValue, boolean isFinalIdentifier) {
        return false;
    }

    public BigDecimal getNumericValue() {
        return null;
    }

    public boolean isTrue() {
        return false;
    }

    public int size() {
        return 0;
    }

    public Value iterator() {
        return null;
    }

    public String toString() {
        return "instance of " + getClass().getSimpleName();
    }

    public static <T extends CustomType> T createInstance(Class<T> type, List<Value> parameters) {
        final Constructor<?> constructor = Arrays.stream(type.getConstructors())
                .filter(c -> c.getParameterCount() == 1 && c.getParameterTypes()[0].equals(List.class))
                .findFirst()
                .orElse(null);

        if (constructor == null) {
            throw new MenterExecutionException("Custom type " + type.getName() + " does not have a constructor with List<Value> parameter");
        }

        try {
            return (T) constructor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            final String menterExecutionMessage = getMenterExecutionMessage(e);
            throw new MenterExecutionException("Could not instantiate custom type " + type.getSimpleName() + "()" + (menterExecutionMessage != null ? ":\n" + menterExecutionMessage : ""), e);
        }
    }

    public List<Method> findReflectiveMethods() {
        return Arrays.stream(getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(TypeFunction.class))
                .collect(Collectors.toList());
    }

    public List<String> findReflectiveMethodNames() {
        return Arrays.stream(getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(TypeFunction.class))
                .map(Method::getName)
                .collect(Collectors.toList());
    }

    public static Method findReflectiveMethod(Class<CustomType> clazz, String functionName) {
        final Method method = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(TypeFunction.class))
                .filter(m -> m.getName().equals(functionName))
                .findFirst()
                .orElse(null);
        if (method == null) {
            throw new MenterExecutionException("Function [" + functionName + "] does not exist on [" + clazz.getTypeName() + "]");
        }
        return method;
    }

    public Method findReflectiveMethod(String functionName) {
        return findReflectiveMethod((Class<CustomType>) getClass(), functionName);
    }

    public Value callReflectiveMethod(Method method, List<Value> parameters) {
        if (method == null) {
            throw new MenterExecutionException("Cannot call null method on " + this);
        }
        try {
            return (Value) method.invoke(this, parameters);
        } catch (Exception e) {
            final String menterExecutionMessage = getMenterExecutionMessage(e);
            throw new MenterExecutionException("The custom type [" + getClass().getSimpleName() + "] could not call function " + method.getName() + "()" + (menterExecutionMessage != null ? ":\n" + menterExecutionMessage : ""), e);
        }
    }

    private static String getMenterExecutionMessage(Exception e) {
        // find the MenterExecutionException in the cause chain and get the message (if present)
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof MenterExecutionException) {
                return cause.getMessage();
            }
            cause = cause.getCause();
        }
        return null;
    }

    protected int checkParameterCombination(List<Value> parameters, String[][] types) {
        final Boolean[] availableTypes = IntStream.range(0, types.length).mapToObj(e -> Boolean.TRUE).toArray(Boolean[]::new);

        for (int i = 0; i < parameters.size(); i++) {
            final Value parameter = parameters.get(i);
            final String parameterType = parameter.getType();

            for (int j = 0; j < availableTypes.length; j++) {
                if (!availableTypes[j]) continue;

                final String[] type = types[j];
                if (type.length <= i) {
                    availableTypes[j] = false;
                    continue;
                }
                if (!type[i].equals(parameterType) && !type[i].equals(PrimitiveValueType.ANY.getType())) {
                    availableTypes[j] = false;
                }
            }
        }

        int longestChain = 0;
        int longestChainIndex = -1;
        for (int i = 0; i < availableTypes.length; i++) {
            if (!availableTypes[i]) continue;
            final String[] type = types[i];
            if (type.length > longestChain) {
                longestChain = type.length;
                longestChainIndex = i;
            }
        }

        return longestChainIndex;
    }

    protected MenterExecutionException invalidParameterCombinationException(String methodName, List<Value> parameters, String[][] types) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Invalid parameter combination for function [").append(getClass().getSimpleName()).append(".").append(methodName).append("]: ").append(parameters).append("\n");

        sb.append("  Expected:\n");
        for (int i = 0; i < types.length; i++) {
            final String[] type = types[i];
            sb.append("    - ").append(Arrays.toString(type));
            if (i < types.length - 1) {
                sb.append("\n");
            }
        }

        return new MenterExecutionException(sb.toString());
    }

    @Override
    public int compareTo(CustomType value) {
        return 0;
    }
}
