package de.yanwittmann.menter.interpreter.structure;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.util.*;

public class EvaluationContextLocalInformation {

    private final List<Map<String, Value>> localSymbolHierarchy;
    private final Stack<MenterStackTraceElement> stackTrace;

    public EvaluationContextLocalInformation(Map<String, Value> localSymbols, Stack<MenterStackTraceElement> stackTrace) {
        this(new ArrayList<>(), localSymbols, stackTrace);
    }

    public EvaluationContextLocalInformation(List<Map<String, Value>> previousParentLocalSymbols, Map<String, Value> localSymbols, Stack<MenterStackTraceElement> stackTrace) {
        this.localSymbolHierarchy = previousParentLocalSymbols;
        this.localSymbolHierarchy.add(localSymbols);
        this.stackTrace = stackTrace;
    }

    public EvaluationContextLocalInformation(Map<String, Value> localSymbols) {
        this(localSymbols, new Stack<>());
    }

    public void putLocalSymbol(String name, Value value) {
        for (int i = localSymbolHierarchy.size() - 1; i >= 0; i--) {
            final Map<String, Value> parentLocalSymbol = localSymbolHierarchy.get(i);
            if (parentLocalSymbol.containsKey(name)) {
                parentLocalSymbol.put(name, value);
                return;
            }
        }
        localSymbolHierarchy.get(localSymbolHierarchy.size() - 1).put(name, value);
    }

    public void putLocalSymbolOnTop(String name, Value value) {
        localSymbolHierarchy.get(localSymbolHierarchy.size() - 1).put(name, value);
    }

    public void putLocalSymbol(Map<String, Value> localSymbols) {
        localSymbols.forEach(this::putLocalSymbol);
    }

    public void putLocalSymbol(EvaluationContextLocalInformation context) {
        for (Map<String, Value> symbols : context.localSymbolHierarchy) {
            putLocalSymbol(symbols);
        }
    }

    /**
     * Checks if a <code>self</code> Value already exists in the local symbol hierarchy. In this case, a
     * <code>super</code> Value is created and put into the local symbol hierarchy. The <code>self</code> Value is then
     * replaced by the given <code>value</code>.
     *
     * @param value The new <code>self</code> Value.
     */
    public void putSelf(Value value) {
        if (hasLocalSymbol("self")) {
            putLocalSymbolOnTop("super", getLocalSymbol("self"));
        }
        putLocalSymbolOnTop("self", value);
    }

    public Value getLocalSymbol(String name) {
        for (int i = localSymbolHierarchy.size() - 1; i >= 0; i--) {
            final Map<String, Value> parentLocalSymbol = localSymbolHierarchy.get(i);
            if (parentLocalSymbol.containsKey(name)) {
                return parentLocalSymbol.get(name);
            }
        }
        return null;
    }

    public boolean hasLocalSymbol(String name) {
        for (int i = localSymbolHierarchy.size() - 1; i >= 0; i--) {
            final Map<String, Value> parentLocalSymbol = localSymbolHierarchy.get(i);
            if (parentLocalSymbol.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    public void putStackFrame(GlobalContext context, Object token) {
        stackTrace.push(new MenterStackTraceElement(context, token));
        if (nextFunctionName != null) {
            stackTrace.peek().setFunctionName(nextFunctionName);
            nextFunctionName = null;
        }
    }

    private Map<String, Value> getEffectiveLocalSymbols() {
        final Map<String, Value> effectiveLocalSymbols = new HashMap<>();
        for (int i = localSymbolHierarchy.size() - 1; i >= 0; i--) {
            final Map<String, Value> parentLocalSymbol = localSymbolHierarchy.get(i);
            effectiveLocalSymbols.putAll(parentLocalSymbol);
        }
        return effectiveLocalSymbols;
    }

    public MenterStackTraceElement popStackFrame() {
        return stackTrace.pop();
    }

    private String nextFunctionName;

    public void provideFunctionNameForNextStackTraceElement(String originalFunctionName) {
        nextFunctionName = originalFunctionName;
    }

    private void rippleFunctionNamesDownwards() {
        String functionName = null;
        GlobalContext context = null; // if context changes, the function name is not valid anymore
        for (MenterStackTraceElement stackFrame : stackTrace) {
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

    public Stack<MenterStackTraceElement> getStackTrace() {
        return stackTrace;
    }

    public EvaluationContextLocalInformation deriveNewContext() {
        final EvaluationContextLocalInformation info = new EvaluationContextLocalInformation(new ArrayList<>(localSymbolHierarchy), new HashMap<>(), stackTrace);
        info.nextFunctionName = nextFunctionName;
        return info;
    }

    public EvaluationContextLocalInformation deriveNewFunctionContext() {
        final EvaluationContextLocalInformation info = new EvaluationContextLocalInformation(new HashMap<>(), stackTrace);
        info.nextFunctionName = nextFunctionName;
        return info;
    }

    private String formatStackTrace(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message.replaceAll("\\n\tin \\[.+] ?at .+", "").replaceAll("\n\t(Local|Global) symbols: .+", ""));
        rippleFunctionNamesDownwards();

        if (!stackTrace.isEmpty()) {
            final int maxSourceNameLength = stackTrace.stream().max(Comparator.comparingInt(o -> o.buildContextMethodString().length())).map(o -> o.buildContextMethodString().length()).orElse(0);
            for (int i = stackTrace.size() - 1; i >= 0; i--) {
                sb.append("\n\t").append(stackTrace.get(i).toString(maxSourceNameLength));
            }

            final MenterStackTraceElement stackTraceElementOfInterest = stackTrace.peek();
            appendStackTraceSymbols(sb, stackTraceElementOfInterest, false);
        }

        return sb.toString();
    }

    public void appendStackTraceSymbols(StringBuilder sb, MenterStackTraceElement stackTraceElementOfInterest, boolean variableValueDisplayMode) {
        final Map<String, Value> localSymbolsReduced = getEffectiveLocalSymbols();
        final boolean hasContext = stackTraceElementOfInterest.getContext() != null;
        if (hasContext) {
            stackTraceElementOfInterest.getContext().getVariables().forEach((name, value) -> localSymbolsReduced.remove(name));
        }

        if (MenterDebugger.stackTracePrintValues.size() > 0) {
            sb.append("\n\tDebugger symbols:\n\t\t");
            List<String> lines = new ArrayList<>();
            for (String symbol : MenterDebugger.stackTracePrintValues) {
                Value value = localSymbolsReduced.get(symbol);
                if (value == null && hasContext)
                    value = stackTraceElementOfInterest.getContext().getVariables().get(symbol);
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
        } else if (localSymbolsReduced.size() > 0) {
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

    public MenterExecutionException createException(Throwable cause) {
        return createException(cause.getMessage(), cause);
    }

    public MenterExecutionException createException(String message) {
        return new MenterExecutionException(formatStackTrace(message));
    }

    public MenterExecutionException createException(String message, Throwable cause) {
        if (cause instanceof MenterExecutionException && ((MenterExecutionException) cause).hasStackTrace()) {
            return (MenterExecutionException) cause;
        } else {
            return new MenterExecutionException(formatStackTrace(message), cause);
        }
    }

    public void printStackTrace(String message) {
        MenterDebugger.printer.println(formatStackTrace(message));
    }
}
