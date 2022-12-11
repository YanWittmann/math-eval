package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.parser.ParserNode;

import java.util.ArrayList;
import java.util.List;

public class Module {

    private GlobalContext parentContext;
    private final String name;
    private final List<Object> symbols = new ArrayList<>();

    public Module(GlobalContext parentContext, ParserNode exportStatement) {
        this.parentContext = parentContext;
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

    public GlobalContext getParentContext() {
        return parentContext;
    }

    @Override
    public String toString() {
        return "Module{" +
               "name='" + name + '\'' +
               ", symbols=" + symbols +
               '}';
    }
}
