package de.yanwittmann.menter.interpreter.structure.value;

import java.lang.reflect.Method;
import java.util.List;

public class ClassFunctionList {

    private final String name;
    private final List<Method> functions;

    public ClassFunctionList(String name, List<Method> functions) {
        this.name = name;
        this.functions = functions;
    }

    public String getName() {
        return name;
    }

    public List<Method> getFunctions() {
        return functions;
    }

    @Override
    public String toString() {
        return name + " " + functions;
    }
}
