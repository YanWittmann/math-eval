package de.yanwittmann.matheval;

import org.junit.jupiter.api.Test;

class EvalRuntimeTest {

    @Test
    public void evaluateTest() {
        final EvalRuntime runtime = new EvalRuntime();

        eval("TestModule[\"myFunction\"](TestModule[\"myVariable\"], 3, 6)");
        eval("TestModule.myFunction(TestModule.myVariable, 3.9, .6, .f)");
        eval("myFunction(x, y, z) = x + pow(z, 2)\n    - y\n{test = \"test; [ ] {};:-\"}");
        eval("regex = r/regex/ig");
    }

    private void eval(String expression) {
        System.out.println("result: " + new EvalRuntime().evaluate(expression));
    }
}
