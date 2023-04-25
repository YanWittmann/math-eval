package de.yanwittmann.menter.interpreter;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.interpreter.type.Type001;
import de.yanwittmann.menter.operator.Operators;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CustomTypeTest {

    @BeforeAll
    public static void beforeAll() {
        Value.registerCustomValueType(Type001.class);
    }

    @AfterAll
    public static void afterAll() {
        Value.unregisterCustomValueType(Type001.class);
    }

    @Test
    public void type001_1Test() {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        evaluateAndAssertEqual(interpreter, "[2, Hello World]", "" +
                                                                "import test inline\n" +
                                                                "myType = new customType001(2)\n" +
                                                                "result = [myType.getMyValue()]\n" +
                                                                "myType.setMyValue(\"Hello World\")\n" +
                                                                "result[1] = myType.getMyValue()\n" +
                                                                "result");

        Assertions.assertThrows(MenterExecutionException.class,
                () -> interpreter.evaluate("" +
                                           "import test inline\n" +
                                           "myType = new customType001(2)\n" +
                                           "myType.doStuff()"));

        evaluateAndAssertEqual(interpreter, "5", "" +
                                                 "import test inline\n" +
                                                 "myType = new customType001(\"hey\")\n" +
                                                 "if (myType) 5 else 6");

        evaluateAndAssertEqual(interpreter, "6", "" +
                                                 "import test inline\n" +
                                                 "myType = new customType001(\"\")\n" +
                                                 "if (myType) 5 else 6");

        evaluateAndAssertEqual(interpreter, "[t, e, s, t]", "" +
                                                            "import test inline\n" +
                                                            "myType = new customType001(\"test\")\n" +
                                                            "result = []\n" +
                                                            "index = 0\n" +
                                                            "for (i in myType) { result[index] = i; index = index + 1 }\n" +
                                                            "result");

        evaluateAndAssertEqual(interpreter, "static Hello World!", "" +
                                                            "import test inline\n" +
                                                            "customType001.doStuffStatic()");
    }

    private static void evaluateAndAssertEqual(MenterInterpreter interpreter, String expected, String expression) {
        Assertions.assertEquals(expected, interpreter.evaluate(expression).toDisplayString());
    }
}
