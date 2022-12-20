package de.yanwittmann.matheval.interpreter;

import de.yanwittmann.matheval.operator.Operators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

class MenterInterpreterTest {

    @Test
    @Disabled
    public void withFileTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.loadFile(new File("src/test/resources/lang/other/moduleParsing"));
        interpreter.finishLoadingContexts();

        interpreter.evaluate("import math as ma; ma.add(1, 2);");
    }

    @Test
    @Disabled
    public void assignmentsTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());

        interpreter.loadFile(new File("src/test/resources/lang/other/functions.ter"));
        interpreter.finishLoadingContexts();
    }

    @Test
    @Disabled
    public void smallTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        interpreter.evaluate("a.test(x) = x + 1; a.test(5);");
    }

    @Test
    public void mapTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        Assertions.assertEquals("{\"test\": 4, \"z\": \"test\"}", interpreter.evaluate("hello = 4; {test: hello, z: \"test\"}").toDisplayString());
        Assertions.assertEquals("{\"singlestring\": \"val 1\", \"hmm\": 3, \"string concat\": \"val 2\"}", interpreter.evaluate("map.hmm = 3; map[\"singlestring\"] = \"val 1\"; map[\"string\" + \" concat\"] = \"val 2\"; map").toDisplayString());
    }

    @Test
    public void inlineFunctionAccessViaArrayTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        Assertions.assertEquals("4", interpreter.evaluate("test.t = []; test.t[0] = x -> x + x; test.t[0](2);").toDisplayString());
        Assertions.assertEquals("4", interpreter.evaluate("test.t = [x -> x + x]; test.t[0](2)").toDisplayString());
        Assertions.assertEquals("4", interpreter.evaluate("test.t.t = 4; test.t.t;").toDisplayString());
    }

}