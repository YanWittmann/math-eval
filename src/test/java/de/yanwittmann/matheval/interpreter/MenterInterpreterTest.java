package de.yanwittmann.matheval.interpreter;

import de.yanwittmann.matheval.operator.Operators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

class MenterInterpreterTest {

    @Test
    public void multiModulesTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.loadFile(new File("src/test/resources/lang/other/moduleParsing"));
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "6.282", "import other; import common inline; print(other.myAttribute); other.myAttribute;");
        evaluateAndAssertEqual(interpreter, "3", "import math as ma; ma.add(1, 2);");
    }

    @Test
    @Disabled
    public void assignmentsTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());

        interpreter.loadFile(new File("src/test/resources/lang/other/functions.ter"));
        interpreter.finishLoadingContexts();
    }

    @Test
    public void inlineFunctionTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "6", "a.test = x -> x + 1; a.test(5);");
    }

    @Test
    public void mapTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "{test: 4, z: test}", "hello = 4; {test: hello, z: \"test\"}");
        evaluateAndAssertEqual(interpreter, "{hmm: 3, singlestring: val 1, string concat: val 2}", "map.hmm = 3; map[\"singlestring\"] = \"val 1\"; map[\"string\" + \" concat\"] = \"val 2\"; map");
    }

    @Test
    public void newAccessorTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "4", "test.t = []; test.t[0] = x -> x + x; test.t[0](2);");
        evaluateAndAssertEqual(interpreter, "4", "test.t = [x -> x + x]; test.t[0](2)");
        evaluateAndAssertEqual(interpreter, "4", "test.t.t = 4; test.t.t;");
        evaluateAndAssertEqual(interpreter, "2", "test = {t:1,z:0}; test.keys().size();");
        evaluateAndAssertEqual(interpreter, "2", "{a:1, b:0}.size();");

        evaluateAndAssertEqual(interpreter, "2", "test={a:1, b:0};test.keys().size();");
        evaluateAndAssertEqual(interpreter, "2", "{a:1, b:0}.keys().size();");
        evaluateAndAssertEqual(interpreter, "2", "[2, 3].keys().size()");

        evaluateAndAssertEqual(interpreter, "[0]", "foo(x) = x + x; mapper(f, val) = f(val); [mapper(foo, 3)].keys()");
    }

    @Test
    public void correctMapElementOrder() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "[{key: 0, value: 0}, {key: 1, value: 1}, {key: 2, value: 2}]", "[0, 1, 2].entries()");
    }

    @Test
    public void objectFunctionsTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "[3, 2, 1]", "[1, 2, 3].sort((a, b) -> b - a)");
        evaluateAndAssertEqual(interpreter, "[1, 2, 3]", "[1, 2, 3].sort((a, b) -> a - b)");

        evaluateAndAssertEqual(interpreter, "~ 8 - 7 ~", "[1, 2, 3, 3].map(x -> x + 5).filter(x -> x > 6).sort((a, b) -> b - a).distinct().join(\" - \", \"~ \", \" ~\")");
    }

    private static void evaluateAndAssertEqual(MenterInterpreter interpreter, String expected, String expression) {
        Assertions.assertEquals(expected, interpreter.evaluate(expression).toDisplayString());
    }

    @Test
    @Disabled
    public void currentTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        MenterDebugger.logParseProgress = true;
        MenterDebugger.logParsedTokens = true;
        MenterDebugger.logInterpreterEvaluation = true;
        MenterDebugger.logInterpreterResolveSymbols = true;

        evaluateAndAssertEqual(interpreter, "", "import core as c; c.print(\"f\")");
    }

}