package de.yanwittmann.matheval.interpreter;

import de.yanwittmann.matheval.operator.Operators;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

class InterpreterTest {

    @Test
    @Disabled
    public void test() {
        Interpreter interpreter = new Interpreter(new Operators());
        // Interpreter.setDebugMode(true);
        interpreter.loadFile(new File("src/test/resources/lang/other/moduleParsing"));
        interpreter.finish();

        interpreter.evaluate("import math as ma; ma.add(1, 2);");
    }

}