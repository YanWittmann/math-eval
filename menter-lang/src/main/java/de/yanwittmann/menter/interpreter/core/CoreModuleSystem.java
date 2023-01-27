package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.util.List;
import java.util.stream.Collectors;

public class CoreModuleSystem {

    static {
        EvaluationContext.registerNativeFunction("system.mtr", "print", CoreModuleSystem::print);
        EvaluationContext.registerNativeFunction("system.mtr", "range", CoreModuleSystem::getProperty);
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

    public static Value print(List<Value> arguments) {
        MenterDebugger.printer.println(arguments.stream()
                .map(v -> v.toDisplayString())
                .collect(Collectors.joining(" ")));
        return Value.empty();
    }
}
