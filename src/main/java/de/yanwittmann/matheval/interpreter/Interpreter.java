package de.yanwittmann.matheval.interpreter;

import de.yanwittmann.matheval.EvalRuntime;
import de.yanwittmann.matheval.parser.Parser;

public class Interpreter {

    private final EvalRuntime runtime;
    private final Parser parser;

    public Interpreter(EvalRuntime runtime, Parser parser) {
        this.runtime = runtime;
        this.parser = parser;
    }

    public Object evaluate(String expression) {
        return null;
    }
}
