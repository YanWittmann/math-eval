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
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MenterInterpreter extends EvalRuntime {

    public static final String VERSION = "0.0.1-SNAPSHOT";
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

    private static Map<String, List<String>> extractCommandLineArguments(String[] args, String[][] argumentNamesAliases) {
        final Map<String, List<String>> arguments = new HashMap<>();

        String currentArgumentName = null;

        for (final String arg : args) {
            String normalizedArgName = null;
            for (String[] argumentNamesAlias : argumentNamesAliases) {
                for (String argumentName : argumentNamesAlias) {
                    if (arg.equals(argumentName)) {
                        normalizedArgName = argumentNamesAlias[0];
                        break;
                    }
                }
                if (normalizedArgName != null) break;
            }

            if (normalizedArgName != null) {
                currentArgumentName = normalizedArgName;
                arguments.put(currentArgumentName, new ArrayList<>());
                continue;
            }

            if (currentArgumentName == null) {
                throw new MenterExecutionException("Invalid argument: " + arg);
            }

            arguments.get(currentArgumentName).add(arg);
        }

        return arguments;
    }

    public static void main(String[] args) {
        final Map<String, List<String>> commandLineArguments = new LinkedHashMap<>();
        try {
            commandLineArguments.putAll(extractCommandLineArguments(args, new String[][]{
                    {"-h", "--help"},
                    {"-v", "--version"},
                    {"-f", "--file"},
                    {"-e", "--eval"},
                    {"-repl", "--repl", "repl"},
                    {"-gs", "--guide-server", "guide-server"},
            }));

        } catch (MenterExecutionException e) {
            for (String arg : args) {
                final File file = new File(arg);
                if (file.exists() && file.isFile()) {
                    commandLineArguments.put("-f", Collections.singletonList(arg));
                    break;
                }
            }

            if (commandLineArguments.isEmpty()) {
                System.out.println(e.getMessage());
                commandLineArguments.put("-h", Collections.emptyList());
            }
        }

        final MenterInterpreter interpreter = new MenterInterpreter(new Operators());

        final boolean isRepl = commandLineArguments.containsKey("-repl");
        final boolean isGuideServer = commandLineArguments.containsKey("-gs");
        final boolean isGuideServerUnsafe = commandLineArguments.containsKey("-gs") && (commandLineArguments.get("-gs").contains("unsafe") || commandLineArguments.get("-gs").contains("us"));
        final int guideServerPort = commandLineArguments.containsKey("-gs") &&
                                    commandLineArguments.get("-gs").stream().anyMatch(s -> s.matches("\\d+"))
                ? Integer.parseInt(commandLineArguments.get("-gs").stream().filter(s -> s.matches("\\d+")).findFirst().get())
                : -1;

        final List<File> files = new ArrayList<>();
        if (commandLineArguments.containsKey("-f")) {
            for (String file : commandLineArguments.get("-f")) {
                files.add(new File(file));
            }
        }
        final boolean hasFiles = files.size() > 0;

        final boolean isHelp = commandLineArguments.containsKey("-h") || (!isRepl && !hasFiles && !isGuideServer);

        if (isHelp) {
            MenterDebugger.printer.println("Menter Interpreter");
            MenterDebugger.printer.println("  [-f, --file] <file> ... - load Menter source files");
            MenterDebugger.printer.println("  [-e, --eval] <code> - evaluate Menter code");
            MenterDebugger.printer.println("  [-repl, --repl, repl] - start REPL");
            MenterDebugger.printer.println("  [-gs, --guide-server, guide-server] <unsafe, us> <port> - start guide server (unsafe mode, port)");
            MenterDebugger.printer.println("  [-h, --help] - print this help");
            MenterDebugger.printer.println("  [-v, --version] - print version");
            MenterDebugger.printer.println();
            return;
        }

        if (hasFiles) {
            for (File file : files) {
                interpreter.loadFile(file);
            }
            interpreter.finishLoadingContexts();
        }

        interpreter.getModuleOptions().addAutoImport("common inline");

        if (isGuideServer) {
            try {
                new MenterGuideServer(interpreter, !isGuideServerUnsafe, guideServerPort);
            } catch (IOException e) {
                LOG.error("Failed to start guide server", e);
            }

        } else if (isRepl) {
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
}
