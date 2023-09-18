package de.yanwittmann.menter.interpreter.structure.value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

public class ClassFunctionList {

    private final String name;
    private final Value calledOn;
    private final List<Method> functions;

    public ClassFunctionList(Value calledOn, String name, List<Method> functions) {
        this.name = name;
        this.calledOn = calledOn;
        this.functions = functions;
    }

    public String getName() {
        return name;
    }

    public Value getCalledOn() {
        return calledOn;
    }

    public List<Method> getFunctions() {
        return functions;
    }

    public Method findMethod(List<Value> arguments) {
        Method bestMethod = null;
        int bestScore = -1;

        for (Method method : functions) {
            final Class<?>[] parameterTypes = method.getParameterTypes();

            if (parameterTypes.length != arguments.size()) {
                continue;
            }

            int currentScore = 0;

            for (int i = 0; i < parameterTypes.length; i++) {
                final Class<?> parameterType = parameterTypes[i];
                final Value argument = arguments.get(i);
                final Class<?> argumentType = argument.getValue().getClass();

                if (parameterType.equals(argumentType)) {
                    // exact match
                    currentScore += 2;
                } else if (Number.class.isAssignableFrom(parameterType) || parameterType.isPrimitive()) {
                    // check for Number types
                    if (PrimitiveValueType.isType(argument, PrimitiveValueType.NUMBER) || parameterType.isAssignableFrom(argumentType)) {
                        currentScore += 1;
                    }
                } else if (parameterType.isAssignableFrom(argumentType)) {
                    // assignable match
                    currentScore += 1;
                }
            }

            if (currentScore > bestScore) {
                bestMethod = method;
                bestScore = currentScore;
            }
        }

        return bestMethod;
    }

    public static Object[] buildArgumentList(List<Value> functionParameters, Class<?>[] parameterTypes) {
        final Object[] invokeArgs = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            final Class<?> parameterType = parameterTypes[i];
            final Value argument = functionParameters.get(i);

            if (Number.class.isAssignableFrom(parameterType) || parameterType.isPrimitive()) {
                if (PrimitiveValueType.isType(argument, PrimitiveValueType.NUMBER)) {
                    BigDecimal bigDecimalValue = (BigDecimal) argument.getValue();
                    if (parameterType.equals(int.class) || parameterType.equals(Integer.class)) {
                        invokeArgs[i] = bigDecimalValue.intValue();
                    } else if (parameterType.equals(long.class) || parameterType.equals(Long.class)) {
                        invokeArgs[i] = bigDecimalValue.longValue();
                    } else if (parameterType.equals(double.class) || parameterType.equals(Double.class)) {
                        invokeArgs[i] = bigDecimalValue.doubleValue();
                    } else {
                        invokeArgs[i] = argument.getValue();
                    }
                } else {
                    invokeArgs[i] = argument.getValue();
                }
            } else {
                invokeArgs[i] = argument.getValue();
            }
        }
        return invokeArgs;
    }

    @Override
    public String toString() {
        return calledOn + "." + name + " " + functions;
    }

    public static Constructor<?> findBestMatchingConstructor(Class<?> clazz, List<Value> arguments) {
        Constructor<?> bestConstructor = null;
        int bestScore = -1;

        for (Constructor<?> constructor : clazz.getConstructors()) {
            final Class<?>[] parameterTypes = constructor.getParameterTypes();

            if (parameterTypes.length != arguments.size()) {
                continue;
            }

            int currentScore = 0;

            for (int i = 0; i < parameterTypes.length; i++) {
                final Class<?> parameterType = parameterTypes[i];
                final Value argument = arguments.get(i);
                final Class<?> argumentType = argument.getValue().getClass();

                if (parameterType.equals(argumentType)) {
                    currentScore += 2;
                } else if (Number.class.isAssignableFrom(parameterType) || parameterType.isPrimitive()) {
                    if (PrimitiveValueType.isType(argument, PrimitiveValueType.NUMBER) || parameterType.isAssignableFrom(argumentType)) {
                        currentScore += 1;
                    }
                } else if (parameterType.isAssignableFrom(argumentType)) {
                    currentScore += 1;
                }
            }

            if (currentScore > bestScore) {
                bestConstructor = constructor;
                bestScore = currentScore;
            }
        }

        return bestConstructor;
    }

}
