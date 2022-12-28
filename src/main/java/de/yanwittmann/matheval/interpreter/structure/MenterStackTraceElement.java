package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.parser.ParserNode;

public class MenterStackTraceElement {

    private String functionName;
    private final GlobalContext context;
    private final Object token;

    public MenterStackTraceElement(GlobalContext context, Object token) {
        this.context = context;
        this.token = token;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public GlobalContext getContext() {
        return context;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String buildContextMethodString() {
        if (functionName == null) return context.getSourceName();
        return context.getSourceName() + "." + functionName;
    }

    public String toString(int maxSourceNameLength) {
        return String.format("in [%-" + maxSourceNameLength + "s] at %s", buildContextMethodString(), ParserNode.reconstructCode(token));
    }
}
