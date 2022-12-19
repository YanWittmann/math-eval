package de.yanwittmann.matheval.interpreter;

import de.yanwittmann.matheval.EvalRuntime;
import de.yanwittmann.matheval.exceptions.InterpreterException;
import de.yanwittmann.matheval.operator.Operators;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MenterInterpreter extends EvalRuntime {

    private static final Logger LOG = LogManager.getLogger(MenterInterpreter.class);

    public MenterInterpreter(Operators operators) {
        super(operators);
        loadMenterCoreFiles();
    }

    private final static String[] MENTER_SOURCE_FILES = {
            "core.ter"
    };

    private void loadMenterCoreFiles() {
        try {
            for (String file : MENTER_SOURCE_FILES) {
                loadContext(readLinesFromResource("/src/" + file), file);
            }
            finish();
        } catch (Exception e) {
            throw new InterpreterException("Failed to load Menter core files", e);
        }

        try {
            final File externalMenterHomeDir = firstExistingDirectory(
                    () -> System.getenv("MENTER_HOME"),
                    () -> System.getProperty("menter.home")
            );

            if (externalMenterHomeDir != null) {
                LOG.debug("Loading Menter core files from external Menter home directory: {}", externalMenterHomeDir);
                loadFile(new File(externalMenterHomeDir, "src"));
            }
        } catch (Exception e) {
            throw new InterpreterException("Failed to load additional Menter files. Additional files have been attempted to be loaded from:\n" +
                                           "  - MENTER_HOME environment variable\n" +
                                           "  - menter.home system property", e);
        }
    }

    private File firstExistingDirectory(Supplier<String>... suppliers) {
        for (Supplier<String> supplier : suppliers) {
            final String path = supplier.get();
            if (path != null) {
                final File file = new File(path);
                if (file.exists() && file.isDirectory()) {
                    return file;
                }
            }
        }
        return null;
    }

    private List<String> readLinesFromResource(String path) {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            final String parsed = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return Arrays.stream(parsed.split("\n")).collect(Collectors.toList());
        } catch (Exception e) {
            throw new InterpreterException("Failed to read resource " + path, e);
        }
    }

    public static void main(String[] args) {
        MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.evaluate("import core inline; print(\"Hello World!\")");
    }
}
