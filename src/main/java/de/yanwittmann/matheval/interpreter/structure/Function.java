package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.parser.ParserNode;

import java.util.ArrayList;
import java.util.List;

public class Function {

    private final Object name;
    private final List<Object> parameters = new ArrayList<>();
    private final List<Object> body = new ArrayList<>();

    public Function(ParserNode functionStatement) {
        name = functionStatement.getChildren().get(0);

        final ParserNode array = (ParserNode) functionStatement.getChildren().get(1);
        parameters.addAll(array.getChildren());

        body.addAll(((ParserNode) functionStatement.getChildren().get(2)).getChildren());
    }

    @Override
    public String toString() {
        return name + "(" + parameters + ") = " + body.size() + " statements";
    }
}
