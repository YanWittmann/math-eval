package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.parser.ParserNode;

import java.util.List;
import java.util.stream.Collectors;

public class MFunction {

    private final GlobalContext parentContext;
    private final List<Object> parameters;
    private final ParserNode body;

    public MFunction(GlobalContext parentContext, List<Object> parameters, ParserNode body) {
        this.parentContext = parentContext;
        this.parameters = parameters;
        this.body = body;
    }

    public MFunction(GlobalContext parentContext, List<Object> parameters) {
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
        return "Function{" +
               "parameters=" + parameters +
               ", body=" + (body != null ? body.reconstructCode() : null) +
               '}';
    }
}
