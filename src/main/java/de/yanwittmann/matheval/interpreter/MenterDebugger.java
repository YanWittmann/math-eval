package de.yanwittmann.matheval.interpreter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public abstract class MenterDebugger {

    public static boolean logLexedTokens = false;

    public static boolean logParsedTokens = false;
    public static boolean logParseProgress = false;

    /**
     * 0 = no logging
     * 1 = log top-down evaluation of reconstructed code
     * 2 = log top-town evaluation of reconstructed code and result of each step
     * 3 = log bottom-up evaluation of reconstructed code and result of each step
     */
    public static int logInterpreterEvaluationStyle = 0;
    /**
     * Will cause the program to log a special message when the given string is found in the evaluation.<br>
     * Place your debugger there and set a breakpoint to debug the evaluation.
     */
    public static String breakpointActivationCode = null;
    public static boolean haltOnEveryExecutionStep = false;
    public static boolean logInterpreterResolveSymbols = false;
    public static boolean logInterpreterEvaluationOrder = false;
    public static boolean logInterpreterAssignments = false;

    public static int stackTraceUnknownSymbolSuggestions = 3;
    public static List<String> stackTracePrintValues = new ArrayList<>();

    public static PrintStream printer = System.out;

    public static int waitForDebuggerResume() {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            final String input = reader.readLine();
            switch (input) {
                case "symbols":
                case "s":
                    return 1;
                case "stacktrace":
                case "st":
                case "trace":
                case "stack":
                    return 2;
                case "continue":
                case "resume":
                case "r":
                    return 3;
                default:
                    return 0;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
