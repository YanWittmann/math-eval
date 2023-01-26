package de.yanwittmann.menter.interpreter;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.exceptions.ParsingException;
import de.yanwittmann.menter.operator.Operators;
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

        interpreter.loadFile(new File("src/test/resources/lang/other/functions.mtr"));
        interpreter.finishLoadingContexts();
    }

    @Test
    public void otherTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "6", "a.test = x -> x + 1; a.test(5);");
        evaluateAndAssertEqual(interpreter, "false", "!true\n"); // newlines at the end caused the lexer to read oob
        evaluateAndAssertEqual(interpreter, "true", "[1,2].containsValue(1)"); // contains functions would not compare the values, but the value instances

        evaluateAndAssertEqual(interpreter, "[6, 8]", "import common inline;" +
                                                      "range(1, 4)\n" +
                                                      "  .map(x -> x * 2)\n" +
                                                      "  .filter(x -> x > 4)"); // this would not have worked because of the indentation being the same on the lower two lines

        evaluateAndAssertEqual(interpreter, "[6, 0, [6]]", "fun(x) {\n" +
                                                           "  if (x.type() == \"number\") x + 5\n" +
                                                           "  else if (x.type() == \"object\") x.map(x -> x + 5)\n" +
                                                           "  else 0\n" +
                                                           "}\n" +
                                                           "[1, \"1\", [1]].map(fun)"); // this would fail because of like 3 different reasons (mainly because the brackets on the if statements would be evaluated after the + expressions)

        Assertions.assertThrows(MenterExecutionException.class, () -> interpreter.evaluate("test = 4; test.f = () -> test; test.f() = 43;")); // function calls should not be assignable
        Assertions.assertThrows(ParsingException.class, () -> interpreter.evaluate("a.a(a, b) {}")); // function declaration via object.child() { ... } is not supported
    }

    @Test
    public void returnBreakContinueTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "[1, 0]", "import common inline; test(a,b) { if (a) return b + 1; return b; }; a = [test(true, 0)]; a[1] = test(false, 0); return a");
        evaluateAndAssertEqual(interpreter, "[2, 3]", "arr = []; for (i in [1,2,3, 4]) { if (i <= 1) continue else if (i == 4) break; arr[i - 2] = i }; arr");
    }


    @Test
    public void reflectionTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();
        interpreter.getModuleOptions().addAutoImport("reflect inline");

        evaluateAndAssertEqual(interpreter, "42", "test = 4; fun() = test; inherit(fun(), 42); test");
        evaluateAndAssertEqual(interpreter, "42", "test = [4]; fun = access(test, \"map\"); test = fun(x -> x + 38); test[0]");
        evaluateAndAssertEqual(interpreter, "42", "access({1: 42}, 1)");
    }

    @Test
    public void closureTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "7", "test = if (true) { var = 4; x -> { x + var } }; test(3)");
        evaluateAndAssertEqual(interpreter, "[22, Yan]", "Person = (name, age) -> { functions.getName = () -> name; functions.getAge = () -> age; functions }; [Person(\"Yan\", 22).getAge(), Person(\"Yan\", 22).getName()]");
        evaluateAndAssertEqual(interpreter, "[23, Yan]", "Person = (name, age) -> { p.age = age; p.name = name; functions.getName = () -> p.name; functions.getAge = () -> p.age; functions.altern = () -> p.age = p.age + 1; functions }; yan = Person(\"Yan\", 22); yan.altern(); [yan.getAge(), yan.getName()]");

        evaluateAndAssertEqual(interpreter, "7", "x = 4; f = x -> x + 1; f(6);");

        evaluateAndAssertEqual(interpreter, "7", "creator(a) {test.test = a;f.setTest = (a) -> { test.test = a };f.getTest = () -> { test.test };f}; test = creator(34); test.setTest(7); test.getTest()");

        evaluateAndAssertEqual(interpreter, "[12, 7]", "creator(a) { test.test = a * 3; f.setTest = (a) -> { test.test = a }; f.getTest = () -> { test.test }; f }; created = creator(4); data[0] = created.getTest(); created.setTest(7); data[1] = created.getTest(); data");
    }

    @Test
    public void pipelineOperatorTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "6", "1 |> x -> x + 5");
        evaluateAndAssertEqual(interpreter, "6", "((x, y) -> x + y)(2, 4)");

        evaluateAndAssertEqual(interpreter, "9", "" +
                                                 "add = (x, y) -> x + y;" +
                                                 "double = x -> x * 2;" +
                                                 "math.sin = x -> x + 5;" +
                                                 "math.test = (x, y) -> if (x < y) x else y;" +
                                                 "math.test(9, math.sin(double(add(2, 1))));");

        evaluateAndAssertEqual(interpreter, "19", "" +
                                                  "add = (x, y) -> x + y;" +
                                                  "double = x -> x * 2;" +
                                                  "math.sin = x -> x + 5;" +
                                                  "math.test = (x, y) -> if (x < y) x else y;" +
                                                  "1 |> add(2) |> double |> math.sin |> math.test(100) |> ((x, y) -> x + y)(3) |> x -> x + 5");

        evaluateAndAssertEqual(interpreter, "6", "3 + (4 |> x -> x - 1)");
        evaluateAndAssertEqual(interpreter, "9", "7 |> x -> x + 1 |> x -> x + 1"); // the issue with this one is that the -> would check for operators (|>) behind the next token, which would cancel the -> operator

        evaluateAndAssertEqual(interpreter, "-10 !", "double = x -> x * 2; (([1, 2].map(x -> x + 3) |> x -> [x[0], -x[1]])[1] |> double) + \" !\"");
        evaluateAndAssertEqual(interpreter, "-10 !", "double = x -> x * 2; (([1, 2].map(x -> x + 3) >| x -> [x[0], -x[1]])[1] >| double) + \" !\"");
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

        evaluateAndAssertEqual(interpreter, "3", "(x -> x + 1)(2)");
        evaluateAndAssertEqual(interpreter, "(x) -> { print(x); }", "x -> print(x)");

        evaluateAndAssertEqual(interpreter, "4", "[1, 2].map(x -> x + 3) |> x -> x[0]"); // would not recognize the x[0] as accessed value, as the thisChainIsInvalid flag would never be reset

        evaluateAndAssertEqual(interpreter, "2", "([2, 3])[0]"); // brackets would not be evaluated to a value in the accessor system
    }

    @Test
    public void objectTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "[{key: 0, value: 0}, {key: 1, value: 1}, {key: 2, value: 2}]", "[0, 1, 2].entries()");

        evaluateAndAssertEqual(interpreter, "[3, 2, 1]", "[1, 2, 3].sort((a, b) -> b - a)");
        evaluateAndAssertEqual(interpreter, "[1, 2, 3]", "[1, 2, 3].sort((a, b) -> a - b)");

        evaluateAndAssertEqual(interpreter, "~ 8 - 7 ~", "[1, 2, 3, 3].map(x -> x + 5).filter(x -> x > 6).sort((a, b) -> b - a).distinct().join(\" - \", \"~ \", \" ~\")");

        evaluateAndAssertEqual(interpreter, "5", "[\"test\", \"hello\"].map(x -> x.size()).max()");
        evaluateAndAssertEqual(interpreter, "max", "[\"containsValue\", \"max\", \"test\"].max((a, b) -> b.size() - a.size())");
        evaluateAndAssertEqual(interpreter, "containsValue", "[\"containsValue\", \"max\", \"test\"].max((a, b) -> a.size() - b.size())");
    }

    @Test
    public void conditionalBranchesTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "6", "if (2 == 3) { 5 } else if (2 > 1) { 6 } else { 7 }");
        evaluateAndAssertEqual(interpreter, "7", "if (2 == 3) { 5 } else if (2 < 1) { 6 } else { 7 }");

        evaluateAndAssertEqual(interpreter, "6", "if (2 == 2) if (3 > 4) 5 else 6 else 7");
        evaluateAndAssertEqual(interpreter, "7", "if (1 == 2) if (3 < 4) 5 else 6 else 7");

        evaluateAndAssertEqual(interpreter, "[2, 3]", "" +
                                                      "[1, 2].map(x -> x + 1)");
        evaluateAndAssertEqual(interpreter, "[6, 7]", "" +
                                                      "if (false) 3 + 5\n" +
                                                      "else if (true) [1,2].map(x -> x + 5)");
    }

    @Test
    public void operatorsTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "false", "!true");
        evaluateAndAssertEqual(interpreter, "true", "!(1 == 2)");

        evaluateAndAssertEqual(interpreter, "15", "1 + 2 * (3 + 4)");
        evaluateAndAssertEqual(interpreter, "15", "1 * 5 + 2 * (3 + 2)");
        evaluateAndAssertEqual(interpreter, "-44", "" +
                                                   "-1 * (2 + 3 * 4) + -5 * 6");
        evaluateAndAssertEqual(interpreter, "-75", "" +
                                                   "foo = x -> x; test = x -> x;" +
                                                   "1 + 2 * (3 + 4) - 5 + -5 * (3 + foo(4) * 2) + -30 * 1");
        evaluateAndAssertEqual(interpreter, "-30", "" +
                                                   "foo = x -> x; test = x -> x;" +
                                                   "1 + 2 * (3 + 4) - 5 + -5 * (3 + foo(4) * 2) + test(1 * 5 + 2 * (3 + 2)) * 1");

        evaluateAndAssertEqual(interpreter, "-3.5", "1 + 2 * 3 / 4 % 5 - 6");

        evaluateAndAssertEqual(interpreter, "445", "\"4\" + 45"); // would not be string concentrated, but would be parsed as a number

        evaluateAndAssertEqual(interpreter, "5", "(+)(2, 3)");
        evaluateAndAssertEqual(interpreter, "[false, true]", "[true, false].map([!))");
        evaluateAndAssertEqual(interpreter, "[1, 2, 6, 24]", "[1, 2, 3, 4].map((!])");
    }

    @Test
    public void concatenateOperatorTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "[1, 2, 3, 4]", "[1, 2] :: [3, 4]");
        evaluateAndAssertEqual(interpreter, "[1, 2, 3]", "[1, 2] :: 3");
        evaluateAndAssertEqual(interpreter, "[1, 2, 3]", "1 :: [2, 3]");
        evaluateAndAssertEqual(interpreter, "[1, 2]", "1 :: 2");

        evaluateAndAssertEqual(interpreter, "{1: 1, 2: 2, 3: 3}", "{1: 1, 2: 2} :: 3");
        evaluateAndAssertEqual(interpreter, "{1: 1, 2: 2, 3: 3}", "{1: 1, 2: 2} :: {3: 3}");
        evaluateAndAssertEqual(interpreter, "{1: 1, 2: 2, 3: 3}", "{1: 1, 2: 2, 3: 4} :: {3: 3}");
    }

    @Test
    public void nullTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "true", "test = null; test.isNull()");
    }

    @Test
    public void fibonacciTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "832040", "" +
                                                      "fibstorage = {}\n" +
                                                      "fib(n) = {\n" +
                                                      "  if (!fibstorage.containsKey(n)) { fibstorage[n] = if (n == 0) 0 else if (n == 1) 1 else fib(n - 1) + fib(n - 2) }\n" +
                                                      "  fibstorage[n]\n" +
                                                      "}\n" +
                                                      "fib(30)");

        evaluateAndAssertEqual(interpreter, "13", "" +
                                                  "fib2 = n -> {\n" +
                                                  " if (n == 0) 0\n" +
                                                  " else if (n == 1) 1\n" +
                                                  " else fib2(n - 1) + fib2 (n - 2)\n" +
                                                  "}\n" +
                                                  "fib2(7)");
    }

    @Test
    public void iteratorTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "6", "" +
                                                 "sum = 0\n" +
                                                 "for (i : [1, 2, 3]) {\n" +
                                                 "  sum = sum + i\n" +
                                                 "}\n" +
                                                 "sum");

        evaluateAndAssertEqual(interpreter, "11", "" +
                                                  "sum = 0\n" +
                                                  "keys = 0\n" +
                                                  "for ((k, v) in [6, 4]) {\n" +
                                                  "  sum = sum + v\n" +
                                                  "  keys = keys + k\n" +
                                                  "}\n" +
                                                  "sum + keys");
    }

    @Test
    public void indentationAutoStatementEndTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "24", "" +
                                                  "sum = 0\n" +
                                                  "for (i in [1, 2, 3]) {\n" +
                                                  "  sum = sum + i; sum = sum + i\n" +
                                                  "  sum = sum + i * 2\n" +
                                                  "}");
    }

    @Test
    public void moduleAttributesTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        interpreter.evaluateInContextOf("d = 5; foo() { d }; export [foo, d] as sometestmodule", "sometestmodule");

        evaluateAndAssertEqual(interpreter, "[d, foo]", "" +
                                                        "import sometestmodule\n" +
                                                        "sometestmodule.symbols.keys()");
    }

    @Test
    public void multipleIdenticalExportsTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        interpreter.evaluateInContextOf("test = 5; export [test] as test", "test-001");
        interpreter.evaluateInContextOf("test = 10; export [test] as test", "test-002");

        evaluateAndAssertEqual(interpreter, "10", "import test; test.test");
    }

    @Test
    public void exceptionTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        interpreter.evaluateInContextOf("test.foo = 4; doStuff(x) { x + calculate(x, 5) }; calculate(a, b) { a + b + test.hmm }; export [test, doStuff] as test", "testContext");

        Assertions.assertThrows(MenterExecutionException.class, () -> evaluateAndAssertEqual(interpreter, "", "import common inline; import test; test.doStuff(5)"));
        Assertions.assertThrows(MenterExecutionException.class, () -> evaluateAndAssertEqual(interpreter, "", "import common inline; import test; print(test.test[1])"));

        try {
            evaluateAndAssertEqual(interpreter, "", "import debug; debug.stackTraceValues(\"a\"); a = 5; test");
        } catch (MenterExecutionException e) {
            Assertions.assertTrue(e.getMessage().contains("a = 5 (number)"));
        }
    }

    @Test
    public void moduleExportTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        interpreter.evaluateInContextOf("sub(x, y) = x - y\nexport [sub] as math", "math");

        evaluateAndAssertEqual(interpreter, "-6",
                "import math\n" +
                "val = x -> {math.sub(-2, x)}\n" +
                "val(4)");
    }

    private static void evaluateAndAssertEqual(MenterInterpreter interpreter, String expected, String expression) {
        Assertions.assertEquals(expected, interpreter.evaluate(expression).toDisplayString());
    }

    @Test
    @Disabled
    public void currentTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        MenterDebugger.logLexedTokens = true;
        MenterDebugger.logParseProgress = true;
        MenterDebugger.logParsedTokens = true;
        MenterDebugger.logInterpreterEvaluationStyle = 2;
        // MenterDebugger.logInterpreterResolveSymbols = true;

        evaluateAndAssertEqual(interpreter, "9", "7 |> x -> x + 1 |> x -> x + 1");
    }

}
