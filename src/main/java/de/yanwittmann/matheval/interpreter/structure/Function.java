package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.parser.ParserNode;

import java.util.List;
import java.util.stream.Collectors;

public class Function {

    private final List<Object> parameters;
    private final ParserNode body;

    public Function(List<Object> parameters, ParserNode body) {
        this.parameters = parameters;
        this.body = body;
    }

    public List<String> getArgumentNames() {
        return parameters.stream().map(o -> ((Token) o).getValue()).collect(Collectors.toList());
    }

    public ParserNode getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "Function{" +
               "parameters=" + parameters +
               ", body=" + body +
               '}';
    }
}
