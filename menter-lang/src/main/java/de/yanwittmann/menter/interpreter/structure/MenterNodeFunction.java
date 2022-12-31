package de.yanwittmann.menter.interpreter.structure;

import de.yanwittmann.menter.lexer.Token;
import de.yanwittmann.menter.parser.ParserNode;

import java.util.List;
import java.util.stream.Collectors;

public class MenterNodeFunction {

    private final GlobalContext parentContext;
    private final List<Object> parameters;
    private final ParserNode body;

    public MenterNodeFunction(GlobalContext parentContext, List<Object> parameters, ParserNode body) {
        this.parentContext = parentContext;
        this.parameters = parameters;
        this.body = body;
    }

    public MenterNodeFunction(GlobalContext parentContext, List<Object> parameters) {
        this.parentContext = parentContext;
        this.parameters = parameters;
        this.body = null;
    }

    public List<String> getArgumentNames() {
        return parameters.stream().map(o -> ((Token) o).getValue()).collect(Collectors.toList());
    }

    public ParserNode getBody() {
        return body;
    }

    public GlobalContext getParentContext() {
        return parentContext;
    }

    @Override
    public String toString() {
        return "(" + parameters.stream().map(Value::toDisplayString).collect(Collectors.joining(", ")) + ") -> " + (body != null ? body.reconstructCode() : null);
    }
}
