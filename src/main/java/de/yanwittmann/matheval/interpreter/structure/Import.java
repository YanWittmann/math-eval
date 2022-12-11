package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.parser.ParserNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Can either be:<br>
 * import TestModule<br>
 * import TestModule as TM<br>
 * import TestModule inline
 */
public class Import {

    private Object nameOrModule;
    private final String alias;
    private final boolean inline;

    public Import(ParserNode node) {
        nameOrModule = node.getChildren().get(0) instanceof Token ? ((Token) node.getChildren().get(0)).getValue() : node.getChildren().get(0);
        if (node.getChildren().size() == 2) {
            alias = node.getChildren().get(1) instanceof Token ? ((Token) node.getChildren().get(1)).getValue() : null;
        } else {
            alias = null;
        }
        inline = node.getType() == ParserNode.NodeType.IMPORT_INLINE_STATEMENT;
    }

    public Import(String name, String alias, boolean inline) {
        this.nameOrModule = name;
        this.alias = alias;
        this.inline = inline;
    }

    public Import(Module module, String alias, boolean inline) {
        this.nameOrModule = module;
        this.alias = alias;
        this.inline = inline;
    }

    public String getName() {
        return nameOrModule instanceof String ? (String) nameOrModule : null;
    }

    public Module getModule() {
        return nameOrModule instanceof Module ? (Module) nameOrModule : null;
    }

    public String getAlias() {
        return alias;
    }

    public boolean isInline() {
        return inline;
    }

    public boolean isModule() {
        return nameOrModule instanceof Module;
    }

    public boolean isName() {
        return nameOrModule instanceof String;
    }

    public String getAliasOrName() {
        return alias != null ? alias : getName();
    }

    protected void findModule(List<GlobalContext> globalContexts) {
        for (GlobalContext globalContext : globalContexts) {
            for (Module module : globalContext.getModules()) {
                if (module.getName().equals(getName())) {
                    nameOrModule = module;
                    return;
                }
            }
        }

        throw new MenterExecutionException("Could not find module '" + getName() + "'. Modules available: " + globalContexts.stream().flatMap(context -> context.getModules().stream()).map(Module::getName).collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "import " + nameOrModule + (alias != null ? " as " + alias : "") + (inline ? " inline" : "");
    }
}
