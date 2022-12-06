package de.yanwittmann.matheval.interpreter;

import de.yanwittmann.matheval.operator.Operators;
import org.junit.jupiter.api.Test;

import java.io.File;

class InterpreterTest {

    @Test
    public void test() {
        Interpreter interpreter = new Interpreter(new Operators());
        Interpreter.setDebugMode(true);
        interpreter.loadFile(new File("src/test/resources/lang/other/fib.ter"));
    }

}