package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.exceptions.ContextInitializationException;
import de.yanwittmann.matheval.parser.ParserNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Context {

    private final Object source;
    private final ParserNode root;
    private final List<Module> modules = new ArrayList<>();
    private final List<Import> imports = new ArrayList<>();

    public Context(ParserNode root, Object source) {
        this.root = root;
        this.source = source;

        for (Object child : root.getChildren()) {
            if (child instanceof ParserNode) {
                final ParserNode childNode = (ParserNode) child;
                if (childNode.getType() == ParserNode.NodeType.EXPORT_STATEMENT) {
                    modules.add(new Module(childNode));
                } else if (childNode.getType() == ParserNode.NodeType.IMPORT_STATEMENT ||
                           childNode.getType() == ParserNode.NodeType.IMPORT_INLINE_STATEMENT ||
                           childNode.getType() == ParserNode.NodeType.IMPORT_AS_STATEMENT) {
                    imports.add(new Import(childNode));
                }
            }
        }

        root.getChildren().removeIf(child -> {
            if (child instanceof ParserNode) {
                final ParserNode childNode = (ParserNode) child;
                return childNode.getType() == ParserNode.NodeType.EXPORT_STATEMENT ||
                       childNode.getType() == ParserNode.NodeType.IMPORT_STATEMENT ||
                       childNode.getType() == ParserNode.NodeType.IMPORT_INLINE_STATEMENT ||
                       childNode.getType() == ParserNode.NodeType.IMPORT_AS_STATEMENT;
            }
            return false;
        });

        {
            final Set<String> moduleNames = new HashSet<>();
            for (Module module : modules) {
                if (!moduleNames.add(module.getName())) {
                    throw new ContextInitializationException("Duplicate module name: " + module.getName());
                }
            }
        }
        {
            final Set<String> importNames = new HashSet<>();
            for (Import anImport : imports) {
                if (anImport.getAlias() != null) {
                    if (!importNames.add(anImport.getAlias())) {
                        throw new ContextInitializationException("Duplicate import alias: " + anImport.getAlias());
                    }
                } else if (anImport.getName() != null) {
                    if (!importNames.add(anImport.getName())) {
                        throw new ContextInitializationException("Duplicate import name: " + anImport.getName());
                    }
                }
            }
        }

        System.out.println(this + "\n");
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Import> getImports() {
        return imports;
    }

    private boolean isFinished = false;

    public void finish(List<Context> contexts) {
        if (isFinished) return;
        isFinished = true;

        for (Import anImport : imports) {
            anImport.findModule(contexts);
        }
    }

    public Object evaluate() {
        return null;
    }

    @Override
    public String toString() {
        final String rootChildren = root.getChildren().stream().map(e -> e instanceof ParserNode ? ((ParserNode) e).getType() : e).collect(Collectors.toList()).toString();
        return "Context '" + source + "':\n" +
               "  modules: " + modules + "\n" +
               "  imports: " + imports + "\n" +
               "  children: " + rootChildren;
    }
}
