package de.yanwittmann.menter.interpreter;

import de.yanwittmann.menter.EvalRuntime;
import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.structure.Value;
import de.yanwittmann.menter.operator.Operators;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
            "common.mtr",
            "io.mtr",
            "system.mtr",
            "debug.mtr",
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
                loadFile(externalMenterHomeDir);
            }

            finishLoadingContexts();
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
        interpreter.getModuleOptions().addAutoImport("common inline");

        final boolean isRepl = MenterInterpreter.isAnyOf(args, "-r", "--repl", "repl");
        final boolean isGuideServer = MenterInterpreter.isAnyOf(args, "-gs", "--guide-server", "guide-server");
        final String isGuideServerUnsafeParam = MenterInterpreter.getArgumentValue(args, "-gs", "--guide-server", "guide-server");
        final boolean isGuideServerUnsafe = isGuideServerUnsafeParam != null && (isGuideServerUnsafeParam.equalsIgnoreCase("unsafe") || isGuideServerUnsafeParam.equalsIgnoreCase("us"));

        final List<File> files = MenterInterpreter.getFiles(args);
        final boolean hasFiles = files.size() > 0;

        final boolean isHelp = MenterInterpreter.isAnyOf(args, "-h", "--help") || (!isRepl && !hasFiles && !isGuideServer);

        if (isHelp) {
            MenterDebugger.printer.println("Menter Interpreter");
            MenterDebugger.printer.println("  -ef [filename]                    Evaluate file");
            MenterDebugger.printer.println("  -r, --repl, repl:                 Start REPL");
            MenterDebugger.printer.println("  -gs, --guide-server [unsafe, us]  Start guide server and whether to allow unsafe imports (io, system, debug)");
            MenterDebugger.printer.println("  -h, --help:                       Show this help");
            return;
        }

        if (hasFiles) {
            for (File file : files) {
                interpreter.loadFile(file);
            }
            interpreter.finishLoadingContexts();
        }

        if (isGuideServer) {
            try {
                new MenterGuideServer(interpreter, isGuideServerUnsafe);
            } catch (IOException e) {
                LOG.error("Failed to start guide server", e);
            }
        }

        if (isRepl) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            boolean debugShowEntireStackTrace = false;
            boolean isMultilineMode = false;
            final List<String> multilineBuffer = new ArrayList<>();

            while (true) {
                try {
                    if (isMultilineMode && multilineBuffer.size() > 0) {
                        MenterDebugger.printer.print("   ");
                    } else {
                        MenterDebugger.printer.print(">> ");
                    }

                    final String input = reader.readLine();
                    if (input == null || input.equals("exit") || input.equals("quit")) break;
                    if (input.trim().length() == 0 && !isMultilineMode) continue;

                    if (input.startsWith("debug ")) {
                        if (input.endsWith("interpreter")) {
                            MenterDebugger.logInterpreterEvaluationStyle = (MenterDebugger.logInterpreterEvaluationStyle + 1) % 3;
                        } else if (input.endsWith("lexer")) {
                            MenterDebugger.logLexedTokens = !MenterDebugger.logLexedTokens;
                        } else if (input.endsWith("parser")) {
                            MenterDebugger.logParsedTokens = !MenterDebugger.logParsedTokens;
                        } else if (input.endsWith("parser progress")) {
                            MenterDebugger.logParseProgress = !MenterDebugger.logParseProgress;
                        } else if (input.endsWith("interpreter resolve")) {
                            MenterDebugger.logInterpreterResolveSymbols = !MenterDebugger.logInterpreterResolveSymbols;
                        } else if (input.endsWith("import order")) {
                            MenterDebugger.logInterpreterEvaluationOrder = !MenterDebugger.logInterpreterEvaluationOrder;
                        } else if (input.endsWith("stack trace")) {
                            debugShowEntireStackTrace = !debugShowEntireStackTrace;
                        } else if (input.endsWith("breakpoint halt")) {
                            MenterDebugger.haltOnEveryExecutionStep = !MenterDebugger.haltOnEveryExecutionStep;
                        } else if (input.contains("breakpoint")) {
                            MenterDebugger.breakpointActivationCode = input.replace("debug breakpoint ", "").trim();
                        } else {
                            MenterDebugger.printer.println("Unknown debug target: " + input.substring(5));
                            MenterDebugger.printer.println("  interpreter         " + MenterDebugger.logInterpreterEvaluationStyle + "\n" +
                                               "  interpreter resolve " + MenterDebugger.logInterpreterResolveSymbols + "\n" +
                                               "  parser              " + MenterDebugger.logParsedTokens + "\n" +
                                               "  parser progress     " + MenterDebugger.logParseProgress + "\n" +
                                               "  lexer               " + MenterDebugger.logLexedTokens + "\n" +
                                               "  import order        " + MenterDebugger.logInterpreterEvaluationOrder + "\n" +
                                               "  stack trace         " + debugShowEntireStackTrace + "\n" +
                                               "  breakpoint          " + MenterDebugger.breakpointActivationCode + "\n" +
                                               "  breakpoint halt     " + MenterDebugger.haltOnEveryExecutionStep);
                        }
                        continue;

                    } else if (input.equals("multiline") || input.equals("ml")) {
                        isMultilineMode = !isMultilineMode;
                        MenterDebugger.printer.println((isMultilineMode ? "Enabled" : "Disabled") + " multiline mode");
                        continue;
                    }

                    final Value result;
                    if (isMultilineMode) {
                        if (input.equals("")) {
                            final String joined = String.join("\n", multilineBuffer);
                            multilineBuffer.clear();
                            isMultilineMode = false;
                            MenterDebugger.haltOnEveryExecutionStep = false;
                            result = interpreter.evaluateInContextOf(joined, "repl");
                        } else {
                            multilineBuffer.add(input);
                            continue;
                        }
                    } else {
                        MenterDebugger.haltOnEveryExecutionStep = false;
                        result = interpreter.evaluateInContextOf(input, "repl");
                    }

                    if (result != null && !result.isEmpty()) {
                        MenterDebugger.printer.println(result.toDisplayString());
                    }
                } catch (Exception e) {
                    if (debugShowEntireStackTrace) {
                        e.printStackTrace();
                    } else {
                        System.err.println("Error: " + e.getMessage());
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    private static List<File> getFiles(String[] args) {
        final List<File> files = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-ef")) {
                if (i + 1 >= args.length) {
                    throw new MenterExecutionException("Menter Interpreter: Expected file name after -ef");
                }
                files.add(new File(args[i + 1]));
                i++;
            }
        }

        if (args.length == 1) {
            final File file = new File(args[0]);
            if (file.exists()) {
                files.add(file);
            }
        }

        for (File file : files) {
            if (!file.exists()) {
                throw new MenterExecutionException("Menter Interpreter: File does not exist: " + file);
            }
        }

        return files;
    }

    private static boolean isAnyOf(String[] args, String... values) {
        return Arrays.stream(args).anyMatch(arg -> Arrays.stream(values).anyMatch(value -> value.equalsIgnoreCase(arg)));
    }

    private static String getArgumentValue(String[] args, String... values) {
        for (int i = 0; i < args.length; i++) {
            int finalI = i;
            if (Arrays.stream(values).anyMatch(value -> value.equalsIgnoreCase(args[finalI]))) {
                if (i + 1 >= args.length) {
                    return null;
                }
                return args[i + 1];
            }
        }
        return null;
    }
}
