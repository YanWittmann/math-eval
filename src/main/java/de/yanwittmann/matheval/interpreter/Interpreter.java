package de.yanwittmann.matheval.interpreter;

import de.yanwittmann.matheval.EvalRuntime;
import de.yanwittmann.matheval.operator.Operators;

public class Interpreter extends EvalRuntime {

    private static boolean debugMode = false;


    public Interpreter(Operators operators) {
        super(operators);
    }

    public static void setDebugMode(boolean debugMode) {
        Interpreter.debugMode = debugMode;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }
}
