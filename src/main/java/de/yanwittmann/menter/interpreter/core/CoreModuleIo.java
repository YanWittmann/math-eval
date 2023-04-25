package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CoreModuleIo {

    static {
        EvaluationContext.registerNativeFunction("io.mtr", "read", CoreModuleIo::apply);
    }

    public static Value apply(List<Value> arguments) {
        try {
            return new Value(FileUtils.readLines(new File(arguments.get(0).getValue().toString()), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MenterExecutionException("Could not read file '" + arguments.get(0).toString() + "'.");
        }
    }
}
