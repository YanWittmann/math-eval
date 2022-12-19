package de.yanwittmann.matheval.interpreter;

import de.yanwittmann.matheval.EvalRuntime;
import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.interpreter.structure.Value;
import de.yanwittmann.matheval.operator.Operators;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MenterInterpreter extends EvalRuntime {

    private static final Logger LOG = LogManager.getLogger(MenterInterpreter.class);

    private final List<String> autoImports = new ArrayList<>();

    public MenterInterpreter(Operators operators) {
        super(operators);
        loadMenterCoreFiles();
    }

    public void addAutoImport(String importStatement) {
        autoImports.add(importStatement);
    }

    public void clearAutoImports() {
        autoImports.clear();
    }

    @Override
    public Value evaluate(String expression) {
        return super.evaluate((autoImports.size() > 0 ? String.join(";", autoImports) + ";" : "") + expression);
    }

    public Value evaluateInContextOf(String expression, String contextSource) {
        return super.evaluateInContextOf((autoImports.size() > 0 ? String.join(";", autoImports) : ""), expression, contextSource);
    }

    @Override
    public Value evaluateInContextOf(String initialExpressions, String expression, String contextSource) {
        return this.evaluateInContextOf(expression, contextSource);
    }

    private final static String[] MENTER_SOURCE_FILES = {
            "core.ter"
    };

    private void loadMenterCoreFiles() {
        try {
            for (String file : MENTER_SOURCE_FILES) {
                loadContext(readLinesFromResource("/src/" + file), file);
            }
            finishLoadingContexts();
        } catch (Exception e) {
            throw new MenterExecutionException("Failed to load Menter core files", e);
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
            throw new MenterExecutionException("Failed to load additional Menter files. Additional files have been attempted to be loaded from:\n" +
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
            throw new MenterExecutionException("Failed to read resource " + path, e);
        }
    }

    public static void main(String[] args) {
        final MenterInterpreter interpreter = new MenterInterpreter(new Operators());
        interpreter.addAutoImport("import core inline");

        final boolean isRepl = MenterInterpreter.isAnyOf(args, "-r", "--repl", "repl");
        final boolean isDebug = MenterInterpreter.isAnyOf(args, "-d", "--debug", "debug");
        final boolean isHelp = MenterInterpreter.isAnyOf(args, "-h", "--help") || (!isRepl && !isDebug);

        if (isHelp) {
            System.out.println("Menter Interpreter");
            System.out.println("  -r, --repl, repl:   Start REPL");
            System.out.println("  -d, --debug, debug: Activate debug mode");
            System.out.println("  -h, --help:         Show this help");
            return;
        }

        if (isDebug) {
            MenterDebugger.logInterpreterEvaluation = true;
            MenterDebugger.logParsedTokens = true;
            MenterDebugger.logLexedTokens = true;
        }

        if (isRepl) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                try {
                    System.out.print(">> ");
                    final String input = reader.readLine();
                    if (input == null || input.equals("exit") || input.equals("quit")) break;
                    if (input.trim().length() == 0) continue;

                    final Value result = interpreter.evaluateInContextOf(input, "repl");
                    if (result != null && !result.isEmpty()) {
                        System.out.println(result.toDisplayString());
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    private static boolean isAnyOf(String[] args, String... values) {
        return Arrays.stream(args).anyMatch(arg -> Arrays.stream(values).anyMatch(value -> value.equalsIgnoreCase(arg)));
    }
}
