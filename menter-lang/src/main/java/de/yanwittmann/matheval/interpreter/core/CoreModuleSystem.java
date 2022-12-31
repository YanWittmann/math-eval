package de.yanwittmann.matheval.interpreter.core;

import de.yanwittmann.matheval.interpreter.structure.Value;

public class CoreModuleSystem {

    public static Value getProperty(Value[] arguments) {
        return new Value(System.getProperty(arguments[0].toString()));

    }

    public static Value getEnv(Value[] arguments) {
        return new Value(System.getenv(arguments[0].toString()));
    }
}
