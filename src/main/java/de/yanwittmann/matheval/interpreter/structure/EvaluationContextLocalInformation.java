package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.interpreter.MenterDebugger;

import java.util.*;

public class EvaluationContextLocalInformation {

    private final Map<String, Value> localSymbols;
    private final Stack<MenterStackTraceElement> stackTrace;

    public EvaluationContextLocalInformation(Map<String, Value> localSymbols, Stack<MenterStackTraceElement> stackTrace) {
        this.localSymbols = localSymbols;
        this.stackTrace = stackTrace;
    }

    public EvaluationContextLocalInformation(Map<String, Value> localSymbols) {
        this(localSymbols, new Stack<>());
    }

    public EvaluationContextLocalInformation() {
        this(new HashMap<>(), new Stack<>());
    }

    public void putLocalSymbol(String name, Value value) {
        localSymbols.put(name, value);
    }

    public void putLocalSymbol(Map<String, Value> localSymbols) {
        this.localSymbols.putAll(localSymbols);
    }

    public Value getLocalSymbol(String name) {
        return localSymbols.get(name);
    }

    public boolean hasLocalSymbol(String name) {
        return localSymbols.containsKey(name);
    }

    public void setLocalSymbols(Map<String, Value> localSymbols) {
        this.localSymbols.clear();
        this.localSymbols.putAll(localSymbols);
    }

    public void putStackFrame(GlobalContext context, Object token) {
        stackTrace.push(new MenterStackTraceElement(context, token));
        if (nextFunctionName != null) {
            stackTrace.peek().setFunctionName(nextFunctionName);
            nextFunctionName = null;
        }
    }

    public MenterStackTraceElement popStackFrame() {
        return stackTrace.pop();
    }

    public void provideFunctionNameForLatestStackTraceElement(String functionName) {
        stackTrace.peek().setFunctionName(functionName);
    }

    private String nextFunctionName;

    public void provideFunctionNameForNextStackTraceElement(String originalFunctionName) {
        nextFunctionName = originalFunctionName;
    }

    private void rippleFunctionNamesDownwards() {
        String functionName = null;
        GlobalContext context = null; // if context changes, the function name is not valid anymore
        for (int i = 0; i < stackTrace.size(); i++) {
            MenterStackTraceElement stackFrame = stackTrace.get(i);
            if (stackFrame.getFunctionName() != null) {
                functionName = stackFrame.getFunctionName();
                context = stackFrame.getContext();
            } else if (stackFrame.getContext() != context) {
                functionName = null;
                context = stackFrame.getContext();
            }
            stackFrame.setFunctionName(functionName);
        }
    }

    public Map<String, Value> getLocalSymbols() {
        return localSymbols;
    }

    public Stack<MenterStackTraceElement> getStackTrace() {
        return stackTrace;
    }

    public EvaluationContextLocalInformation deriveNewContext() {
        final EvaluationContextLocalInformation info = new EvaluationContextLocalInformation(new HashMap<>(localSymbols), stackTrace);
        info.nextFunctionName = nextFunctionName;
        return info;
    }

    private String formatStackTrace(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message.replaceAll("\\n\tin \\[.+] ?at .+", "").replaceAll("\n\t(Local|Global) symbols: .+", ""));
        rippleFunctionNamesDownwards();

        final int maxSourceNameLength = stackTrace.stream().max(Comparator.comparingInt(o -> o.buildContextMethodString().length())).map(o -> o.buildContextMethodString().length()).orElse(0);
        for (int i = stackTrace.size() - 1; i >= 0; i--) {
            sb.append("\n\t").append(stackTrace.get(i).toString(maxSourceNameLength));
        }

        final MenterStackTraceElement stackTraceElementOfInterest = stackTrace.peek();
        appendStackTraceSymbols(sb, stackTraceElementOfInterest, false);

        return sb.toString();
    }

    public void appendStackTraceSymbols(StringBuilder sb, MenterStackTraceElement stackTraceElementOfInterest, boolean variableValueDisplayMode) {
        HashMap<String, Value> localSymbolsReduced = new HashMap<>(localSymbols);
        final boolean hasContext = stackTraceElementOfInterest.getContext() != null;
        if (hasContext) {
            stackTraceElementOfInterest.getContext().getVariables().forEach((name, value) -> localSymbolsReduced.remove(name));
        }

        if (MenterDebugger.stackTracePrintValues.size() > 0) {
            sb.append("\n\tDebugger symbols:\n\t\t");
            List<String> lines = new ArrayList<>();
            for (String symbol : MenterDebugger.stackTracePrintValues) {
                Value value = localSymbolsReduced.get(symbol);
                if (value == null && hasContext) value = stackTraceElementOfInterest.getContext().getVariables().get(symbol);
                if (value == null) value = Value.empty();
                lines.add(symbol + " = " + value);
            }
            sb.append(String.join("\n\t\t", lines));
        }

        if (variableValueDisplayMode) {
            if (localSymbolsReduced.size() > 0) {
                sb.append("\n\tLocal symbols:\n\t\t");
                List<String> lines = new ArrayList<>();
                for (Map.Entry<String, Value> entry : localSymbolsReduced.entrySet()) {
                    lines.add(entry.getKey() + " = " + entry.getValue());
                }
                sb.append(String.join("\n\t\t", lines));
            }
        } else if(localSymbolsReduced.size() > 0) {
            sb.append("\n\tLocal symbols:  ").append(formatVariables(localSymbolsReduced));
        }

        if (hasContext) {
            if (variableValueDisplayMode) {
                if (stackTraceElementOfInterest.getContext().getVariables().size() > 0) {
                    sb.append("\n\tGlobal symbols:\n\t\t");
                    List<String> lines = new ArrayList<>();
                    for (Map.Entry<String, Value> entry : stackTraceElementOfInterest.getContext().getVariables().entrySet()) {
                        lines.add(entry.getKey() + " = " + entry.getValue());
                    }
                    sb.append(String.join("\n\t\t", lines));
                }
            } else if (stackTraceElementOfInterest.getContext().getVariables().size() > 0) {
                sb.append("\n\tGlobal symbols: ").append(formatVariables(stackTraceElementOfInterest.getContext().getVariables()));
            }
        }
    }

    private String formatVariables(Map<String, Value> localSymbols) {
        final String elements = localSymbols.entrySet().stream().limit(20).map(s -> s.getKey() + " (" + s.getValue().getType() + ")").sorted().reduce((s1, s2) -> s1 + ", " + s2).orElse("none");
        if (localSymbols.entrySet().size() > 20) {
            return elements + ", ...";
        } else {
            return elements;
        }
    }

    public MenterExecutionException createException(String message) {
        return new MenterExecutionException(formatStackTrace(message));
    }

    public MenterExecutionException createException(String message, Throwable cause) {
        if (cause instanceof MenterExecutionException && ((MenterExecutionException) cause).hasStackTrace()) {
            return (MenterExecutionException) cause;
        } else {
            return new MenterExecutionException(formatStackTrace(message));
        }
    }

    public void printStackTrace(String message) {
        System.err.println(formatStackTrace(message));
    }
}
