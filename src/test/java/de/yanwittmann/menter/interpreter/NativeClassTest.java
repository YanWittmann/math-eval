package de.yanwittmann.menter.interpreter;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.structure.GlobalContext;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.interpreter.type.NativeTestClass;
import de.yanwittmann.menter.operator.Operators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;

public class NativeClassTest {

    @Test
    public void fileClassTest() {
        try {
            final MenterInterpreter interpreter = new MenterInterpreter(new Operators());
            interpreter.finishLoadingContexts();
            final GlobalContext testContext = interpreter.getOrCreateContext("test");

            Value.registerNativeClass(File.class);

            final File checkFile = new File("test.txt");
            testContext.getVariables().put("someFile", new Value(checkFile));

            Assertions.assertEquals(checkFile.getAbsolutePath(), interpreter.evaluateInContextOf("test", "someFile.getAbsolutePath()").getValue());
            Assertions.assertTrue(interpreter.evaluateInContextOf("test", "someFile.equals(someFile)").isTrue());
        } finally {
            Value.unregisterNativeClass(File.class);
        }
    }

    @Test
    public void customNativeClassModifyTest() {
        try {
            final MenterInterpreter interpreter = new MenterInterpreter(new Operators());
            interpreter.finishLoadingContexts();
            final GlobalContext testContext = interpreter.getOrCreateContext("test");

            Value.registerNativeClass(NativeTestClass.class);

            final NativeTestClass testInstance = new NativeTestClass();
            testContext.getVariables().put("testInstance", new Value(testInstance));

            Assertions.assertEquals(0, ((BigDecimal) interpreter.evaluateInContextOf("test", "testInstance.getTestValue()").getValue()).intValue());
            interpreter.evaluateInContextOf("test", "testInstance.setTestValue(5)");
            Assertions.assertEquals(5, ((BigDecimal) interpreter.evaluateInContextOf("test", "testInstance.getTestValue()").getValue()).intValue());
            testInstance.setTestValue(10);
            Assertions.assertEquals(10, ((BigDecimal) interpreter.evaluateInContextOf("test", "testInstance.getTestValue()").getValue()).intValue());
            interpreter.evaluateInContextOf("test", "testInstance.setIfPasswordCorrect(\"test\", 20.3)");
            Assertions.assertEquals(20, ((BigDecimal) interpreter.evaluateInContextOf("test", "testInstance.getTestValue()").getValue()).doubleValue());
            interpreter.evaluateInContextOf("test", "testInstance.setIfPasswordCorrect(\"wrong\", 40)");
            Assertions.assertEquals(20, ((BigDecimal) interpreter.evaluateInContextOf("test", "testInstance.getTestValue()").getValue()).intValue());
        } finally {
            Value.unregisterNativeClass(NativeTestClass.class);
        }
    }

    @Test
    public void customNativeClassCreateInstanceTest() {
        try {
            final MenterInterpreter interpreter = new MenterInterpreter(new Operators());
            interpreter.finishLoadingContexts();

            Value.registerNativeClass(NativeTestClass.class);

            interpreter.evaluateInContextOf("test", "val = new NativeTestClass()");
            Assertions.assertEquals(0, ((BigDecimal) interpreter.evaluateInContextOf("test", "val.getTestValue()").getValue()).intValue());
            Assertions.assertEquals(0, ((BigDecimal) interpreter.evaluateInContextOf("test", "(new NativeTestClass()).getTestValue()").getValue()).intValue());
            Assertions.assertEquals(20, ((BigDecimal) interpreter.evaluateInContextOf("test", "(new NativeTestClass(20)).getTestValue()").getValue()).intValue());
        } finally {
            Value.unregisterNativeClass(NativeTestClass.class);
        }
    }

    @Test
    public void customNativeClassDoNotRegisterTest() {
        final MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.finishLoadingContexts();

        Assertions.assertThrows(MenterExecutionException.class, () -> {
            interpreter.evaluateInContextOf("test", "val = new NativeTestClass()");
        });
    }

    @Test
    public void customNativeClassAnyClassAccessTest() {
        try {
            final MenterInterpreter interpreter = new MenterInterpreter(new Operators());
            interpreter.finishLoadingContexts();
            final GlobalContext testContext = interpreter.getOrCreateContext("test");

            Value.setAllowAnyNativeClassAccess(true);

            final NativeTestClass testInstance = new NativeTestClass();
            testContext.getVariables().put("testInstance", new Value(testInstance));

            Assertions.assertEquals(0, ((BigDecimal) interpreter.evaluateInContextOf("test", "testInstance.getTestValue()").getValue()).intValue());
            interpreter.evaluateInContextOf("test", "testInstance.setTestValue(5)");
            Assertions.assertEquals(5, ((BigDecimal) interpreter.evaluateInContextOf("test", "testInstance.getTestValue()").getValue()).intValue());

            Assertions.assertThrows(MenterExecutionException.class, () -> {
                interpreter.evaluateInContextOf("test", "val = new NativeTestClass()");
            });
        } finally {
            Value.setAllowAnyNativeClassAccess(false);
        }
    }
}
