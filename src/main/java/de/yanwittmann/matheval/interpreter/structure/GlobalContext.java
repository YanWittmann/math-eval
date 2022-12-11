package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.EvalRuntime;
import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.parser.ParserNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalContext extends EvaluationContext {

    private static final Logger LOG = LogManager.getLogger(GlobalContext.class);

    private final Object source;
    private final ParserNode root;
    private final List<Module> modules = new ArrayList<>();
    private final List<Import> imports = new ArrayList<>();

    public GlobalContext(ParserNode root, Object source) {
        super(null);
        this.root = root;
        this.source = source;

        for (Object child : root.getChildren()) {
            if (child instanceof ParserNode) {
                final ParserNode childNode = (ParserNode) child;

                if (childNode.getType() == ParserNode.NodeType.EXPORT_STATEMENT) {
                    modules.add(new Module(this, childNode));

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
                    throw new MenterExecutionException("Duplicate module name: " + module.getName());
                }
            }
        }
        {
            final Set<String> importNames = new HashSet<>();
            for (Import anImport : imports) {
                if (anImport.getAlias() != null) {
                    if (!importNames.add(anImport.getAlias())) {
                        throw new MenterExecutionException("Duplicate import alias: " + anImport.getAlias());
                    }
                } else if (anImport.getName() != null) {
                    if (!importNames.add(anImport.getName())) {
                        throw new MenterExecutionException("Duplicate import name: " + anImport.getName());
                    }
                }
            }
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Import> getImports() {
        return imports;
    }

    private boolean isFinished = false;

    public void finish(List<GlobalContext> globalContexts) {
        if (isFinished) return;
        isFinished = true;

        for (Import anImport : imports) {
            anImport.findModule(globalContexts);
        }

        evaluate();

        System.out.println(this + "\n");
    }

    public Object evaluate() {
        return super.evaluate(root, this, new HashMap<>(), SymbolCreationMode.THROW_IF_NOT_EXISTS);
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
