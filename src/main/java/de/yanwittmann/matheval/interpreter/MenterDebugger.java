package de.yanwittmann.matheval.interpreter;

public abstract class MenterDebugger {

    public static boolean logLexedTokens = false;

    public static boolean logParsedTokens = false;
    public static boolean logParseProgress = false;

    /**
     * When set to true, will log the reconstructed expression on every evaluation step.
     */
    public static boolean logInterpreterEvaluation = false;
    /**
     * Will cause the program to log a special message when the given string is found in the evaluation.<br>
     * Place your debugger there and set a breakpoint to debug the evaluation.
     */
    public static String breakpointActivationCode = null;
    public static boolean logInterpreterResolveSymbols = false;
    public static boolean logInterpreterEvaluationOrder = false;
}
