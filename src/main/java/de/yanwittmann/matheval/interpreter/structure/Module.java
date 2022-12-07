package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.parser.ParserNode;

import java.util.ArrayList;
import java.util.List;

public class Module {

    private final String name;
    private final List<Object> symbols = new ArrayList<>();

    public Module(ParserNode exportStatement) {
        final ParserNode array = (ParserNode) exportStatement.getChildren().get(0);
        symbols.addAll(array.getChildren());
        name = ((Token) exportStatement.getChildren().get(1)).getValue();
    }

    public String getName() {
        return name;
    }

    public List<Object> getSymbols() {
        return symbols;
    }

    @Override
    public String toString() {
        return "Module{" +
               "name='" + name + '\'' +
               ", symbols=" + symbols +
               '}';
    }
}
