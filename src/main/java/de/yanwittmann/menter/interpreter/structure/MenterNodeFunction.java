package de.yanwittmann.menter.interpreter.structure;

import de.yanwittmann.menter.lexer.Token;
import de.yanwittmann.menter.parser.ParserNode;

import java.util.List;
import java.util.stream.Collectors;

public class MenterNodeFunction {

    private final GlobalContext parentContext;
    private final List<String> parameters;
    private final ParserNode body;

    public MenterNodeFunction(GlobalContext parentContext, List<Object> parameters, ParserNode body) {
        this.parentContext = parentContext;
        this.parameters = parameters.stream().map(o -> ((Token) o).getValue()).collect(Collectors.toList());
        this.body = body;
    }

    public MenterNodeFunction(GlobalContext parentContext, List<Object> parameters) {
        this(parentContext, parameters, null);
    }

    public List<String> getArgumentNames() {
        return parameters;
    }

    public ParserNode getBody() {
        return body;
    }

    public GlobalContext getParentContext() {
        return parentContext;
    }

    @Override
    public String toString() {
        return "(" + String.join(", ", parameters) + ") -> " + (body != null ? body.reconstructCode() : null);
    }
}
