package de.yanwittmann.matheval;

import de.yanwittmann.matheval.interpreter.Interpreter;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.operator.Operators;
import de.yanwittmann.matheval.parser.Parser;

public class EvalRuntime {

    public EvalRuntime() {
    }

    public Object evaluate(String expression) {
        Operators operators = new Operators();
        final Lexer lexer = new Lexer(expression, operators);
        final Parser parser = new Parser(lexer);
        final Interpreter interpreter = new Interpreter(this, parser);
        return interpreter.evaluate(expression);
    }
}
