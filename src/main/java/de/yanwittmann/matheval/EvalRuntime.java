package de.yanwittmann.matheval;

public class EvalRuntime {

    public EvalRuntime() {
    }

    public Object evaluate(String expression) {
        final Lexer lexer = new Lexer(expression);
        final Parser parser = new Parser(lexer);
        final Interpreter interpreter = new Interpreter(this, parser);
        return interpreter.evaluate(expression);
    }
}
