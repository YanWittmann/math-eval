package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.util.List;

public class CoreModuleSystem {

    static {
        EvaluationContext.registerNativeFunction("system.mtr", "getProperty", CoreModuleSystem::getProperty);
        EvaluationContext.registerNativeFunction("system.mtr", "getEnv", CoreModuleSystem::getEnv);
        EvaluationContext.registerNativeFunction("system.mtr", "sleep", CoreModuleSystem::sleep);
    }

    public static Value getProperty(List<Value> arguments) {
        return new Value(System.getProperty(arguments.get(0).toString()));
    }

    public static Value getEnv(List<Value> arguments) {
        return new Value(System.getenv(arguments.get(0).toString()));
    }

    public static Value sleep(List<Value> arguments) {
        try {
            Thread.sleep(arguments.get(0).getNumericValue().longValue());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return arguments.get(0);
    }
}
