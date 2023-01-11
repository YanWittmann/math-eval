package de.yanwittmann.menter.interpreter.structure;

import de.yanwittmann.menter.interpreter.structure.value.CustomType;
import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.lexer.Token;
import de.yanwittmann.menter.parser.ParserNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Module {

    private GlobalContext parentContext;
    private final String name;
    private final List<Object> symbols = new ArrayList<>();
    private final long creationTime;

    public Module(GlobalContext parentContext, ParserNode exportStatement) {
        this.parentContext = parentContext;
        final ParserNode array = (ParserNode) exportStatement.getChildren().get(0);
        symbols.addAll(array.getChildren());
        name = ((Token) exportStatement.getChildren().get(1)).getValue();
        creationTime = System.nanoTime();
    }

    public Module(GlobalContext parentContext, String name) {
        this.parentContext = parentContext;
        this.name = name;
        creationTime = System.nanoTime();
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

    public final static java.util.function.Function<Object, String> ID_TO_KEY_MAPPER = o -> {
        if (o instanceof Token) {
            return ((Token) o).getValue();
        } else if (o instanceof Value) {
            return ((Value) o).getValue().toString();
        } else if (o instanceof String) {
            return (String) o;
        }
        return null;
    };

    public boolean containsSymbol(String symbol) {
        return symbols.stream().map(ID_TO_KEY_MAPPER).anyMatch(s -> s.equals(symbol));
    }

    public void addSymbol(String name) {
        symbols.add(name);
    }

    public long getCreationTime() {
        return creationTime;
    }

    public List<Class<? extends CustomType>> findCustomTypeDefinitions() {
        final List<Class<? extends CustomType>> customTypes = new ArrayList<>();

        for (Map.Entry<String, Value> var : parentContext.getVariables().entrySet()) {
            if (var.getValue().getType().equals(PrimitiveValueType.CUSTOM_TYPE.getType())) {
                final CustomType customType = (CustomType) var.getValue().getValue();
                customTypes.add(customType.getClass());
            }
        }

        return customTypes;
    }

    public boolean containsCustomTypeDefinitionForType(Class<? extends CustomType> type) {
        for (Map.Entry<String, Value> var : parentContext.getVariables().entrySet()) {
            if (var.getValue().getType().equals(PrimitiveValueType.CUSTOM_TYPE.getType())) {
                final CustomType customType = (CustomType) var.getValue().getValue();
                if (customType.getClass().equals(type)) return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "Module{" +
               "name='" + name + '\'' +
               ", symbols=" + symbols +
               '}';
    }
}
