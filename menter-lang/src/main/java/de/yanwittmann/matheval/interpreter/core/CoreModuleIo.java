package de.yanwittmann.matheval.interpreter.core;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.interpreter.structure.Value;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CoreModuleIo {

    public static Value apply(Value[] arguments) {
        try {
            return new Value(FileUtils.readLines(new File(arguments[0].getValue().toString()), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MenterExecutionException("Could not read file '" + arguments[0].toString() + "'.");
        }
    }
}
