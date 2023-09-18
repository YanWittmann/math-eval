package de.yanwittmann.menter.interpreter.structure.value;

import java.lang.reflect.Method;
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

    @Override
    public String toString() {
        return calledOn + "." + name + " " + functions;
    }
}
