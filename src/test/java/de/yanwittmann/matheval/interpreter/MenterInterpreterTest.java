package de.yanwittmann.matheval.interpreter;

import de.yanwittmann.matheval.interpreter.structure.CustomValueType;
import de.yanwittmann.matheval.interpreter.structure.EvaluationContext;
import de.yanwittmann.matheval.interpreter.structure.MenterValueFunction;
import de.yanwittmann.matheval.interpreter.structure.Value;
import de.yanwittmann.matheval.operator.Operators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
    public void otherTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "6", "a.test = x -> x + 1; a.test(5);");
        evaluateAndAssertEqual(interpreter, "false", "!true\n");
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
    }

    @Test
    public void operatorsTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "false", "!true");
        evaluateAndAssertEqual(interpreter, "true", "!1 == 2");
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

        interpreter.evaluateInContextOf("", "d = 5; foo() { d }; export [foo, d] as sometestmodule", "sometestmodule");

        evaluateAndAssertEqual(interpreter, "[d, foo]", "" +
                                                        "import sometestmodule\n" +
                                                        "sometestmodule.symbols.keys()");
    }

    @Test
    public void customTypesTest() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        class TestType {
            private BigDecimal value;
            private List<Value> list = new ArrayList<>();

            public TestType(BigDecimal value) {
                this.value = value;
            }

            public TestType() {
                this.value = BigDecimal.ZERO;
            }
        }

        EvaluationContext.registerNativeFunction(new String[]{"TestType", "TestType"}, values -> {
            if (values.length == 0) {
                return new Value(new TestType());
            } else {
                return new Value(new TestType(((BigDecimal) values[0].getValue())));
            }
        });

        Value.registerCustomValueType(interpreter, new CustomValueType() {
            @Override
            public String getType() {
                return "<TestType>";
            }

            @Override
            public String contextSource() {
                return "native TestType();export [TestType] as TestType";
            }

            @Override
            public HashMap<String, MenterValueFunction> getFunctions() {
                return new HashMap<String, MenterValueFunction>() {
                    {
                        put("getValue", (context, self, values, localSymbols) -> new Value(((TestType) self.getValue()).value));
                        put("setValue", (context, self, values, localSymbols) -> {
                            ((TestType) self.getValue()).value = values.get(0).getNumericValue();
                            return self;
                        });
                        put("getList", (context, self, values, localSymbols) -> new Value(((TestType) self.getValue()).list));
                        put("addToList", (context, self, values, localSymbols) -> {
                            ((TestType) self.getValue()).list.add(values.get(0));
                            return self;
                        });
                        put("size", (context, self, values, localSymbols) -> new Value(self.size()));
                        put("iterator", (context, self, values, localSymbols) -> new Value(((TestType) self.getValue()).list.iterator()));
                    }
                };
            }

            @Override
            public boolean isType(Object value) {
                return value instanceof TestType;
            }

            @Override
            public Value accessValue(Value thisValue, Value identifier) {
                if (thisValue.getValue() instanceof TestType) {
                    if (identifier.getValue() instanceof BigDecimal) {
                        int index = identifier.getNumericValue().intValue();
                        if (index >= 0 && index < ((TestType) thisValue.getValue()).list.size()) {
                            return new Value(((TestType) thisValue.getValue()).list.get(index));
                        }
                    }
                }

                return null;
            }

            @Override
            public boolean createAccessedValue(Value thisValue, Value identifier, Value accessedValue, boolean isFinalIdentifier) {
                if (thisValue.getValue() instanceof TestType) {
                    if (identifier.getValue() instanceof BigDecimal) {
                        int index = ((BigDecimal) identifier.getValue()).intValue();
                        if (index >= 0 && index < ((TestType) thisValue.getValue()).list.size()) {
                            ((TestType) thisValue.getValue()).list.set(index, accessedValue);
                            return true;
                        } else if (index == ((TestType) thisValue.getValue()).list.size()) {
                            ((TestType) thisValue.getValue()).list.add(accessedValue);
                            return true;
                        }
                        return true;
                    }
                }

                return false;
            }

            @Override
            public BigDecimal getNumericValue(Value thisValue) {
                return ((TestType) thisValue.getValue()).value;
            }

            @Override
            public boolean isTrue(Value thisValue) {
                return ((TestType) thisValue.getValue()).value.compareTo(BigDecimal.ZERO) != 0;
            }

            @Override
            public int size(Value thisValue) {
                return ((TestType) thisValue.getValue()).list.size();
            }

            @Override
            public String toDisplayString(Object thisValue) {
                return ((TestType) thisValue).value.toString() + " " + ((TestType) thisValue).list.stream().map(v -> v.toDisplayString()).collect(Collectors.toList());
            }
        });

        interpreter.addAutoImport("TestType inline");
        interpreter.addAutoImport("common inline");

        evaluateAndAssertEqual(interpreter, "3", "" +
                                                 "val = TestType(3)\n" +
                                                 "val.getValue()");

        evaluateAndAssertEqual(interpreter, "5", "" +
                                                 "val = TestType(3)\n" +
                                                 "val.setValue(5)\n" +
                                                 "val.getValue()");

        evaluateAndAssertEqual(interpreter, "[5, 10]", "" +
                                                       "val = TestType()\n" +
                                                       "val[0] = 5\n" +
                                                       "val[1] = 10\n" +
                                                       "val.getList()");

        evaluateAndAssertEqual(interpreter, "<TestType>", "" +
                                                          "val = TestType()\n" +
                                                          "val.type()");

        evaluateAndAssertEqual(interpreter, "4 []", "" +
                                                    "val = TestType(4)\n" +
                                                    "val");

        evaluateAndAssertEqual(interpreter, "1", "" +
                                                 "val = TestType(4)\n" +
                                                 "val.addToList(5)\n" +
                                                 "val.size()");

        evaluateAndAssertEqual(interpreter, "15", "" +
                                                  "val = TestType(1)\n" +
                                                  "val.addToList(5)\n" +
                                                  "val.addToList(10)\n" +
                                                  "sum = 0\n" +
                                                  "if (val) {\n" +
                                                  "  for (i in val) sum = sum + i\n" +
                                                  "}\n" +
                                                  "sum");

        evaluateAndAssertEqual(interpreter, "15", "" +
                                                  "val = TestType(1)\n" +
                                                  "val.addToList(5)\n" +
                                                  "val.addToList(10)\n" +
                                                  "sum = 0\n" +
                                                  "if (val) {\n" +
                                                  "  for (i in range(0, val.size() - 1)) sum = sum + val[i]\n" +
                                                  "}\n" +
                                                  "sum");

        evaluateAndAssertEqual(interpreter, "15", "" +
                                                  "val = TestType(1)\n" +
                                                  "val.addToList(5)\n" +
                                                  "val.addToList(10)\n" +
                                                  "sum = 0\n" +
                                                  "val.forEach(i -> sum = sum + i)\n" +
                                                  "sum");
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
        MenterDebugger.logInterpreterEvaluation = true;
        MenterDebugger.logInterpreterResolveSymbols = true;

        evaluateAndAssertEqual(interpreter, "false", "!true");
    }

}