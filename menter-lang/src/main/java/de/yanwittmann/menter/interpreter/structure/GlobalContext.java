package de.yanwittmann.menter.interpreter.structure;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.parser.ParserNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

public class GlobalContext extends EvaluationContext {

    private static final Logger LOG = LogManager.getLogger(GlobalContext.class);

    private final Object source;
    private final List<Module> modules = new ArrayList<>();
    private final List<Import> imports = new ArrayList<>();

    public GlobalContext(ParserNode root, Object source) {
        super(null);
        this.source = source;

        this.findImportExportStatements(root);
    }

    public void findImportExportStatements(ParserNode root) {
        for (Object child : root.getChildren()) {
            checkForImportStatement(child);
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
            for (int i = imports.size() - 1; i >= 0; i--) {
                Import anImport = imports.get(i);
                final String name = firstNonNull(anImport.getAlias(), anImport.getName());
                if (!importNames.add(name)) {
                    imports.remove(anImport);
                    throw new MenterExecutionException("Duplicate import: " + anImport.getAlias());
                }
            }
        }
    }

    private void checkForImportStatement(Object child) {
        if (child instanceof ParserNode) {
            final ParserNode childNode = (ParserNode) child;

            if (childNode.getType() == ParserNode.NodeType.EXPORT_STATEMENT) {
                modules.add(new Module(this, childNode));

            } else if (childNode.getType() == ParserNode.NodeType.IMPORT_STATEMENT ||
                       childNode.getType() == ParserNode.NodeType.IMPORT_INLINE_STATEMENT ||
                       childNode.getType() == ParserNode.NodeType.IMPORT_AS_STATEMENT) {
                imports.add(new Import(childNode));
                inputsResolved = false;
            }
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Import> getImports() {
        return imports;
    }

    public Object getSource() {
        return source;
    }

    public String getSourceName() {
        if (source instanceof String) {
            return (String) source;
        } else if (source instanceof File) {
            return ((File) source).getName();
        }
        return null;
    }

    private boolean inputsResolved = false;

    public void resolveImports(List<GlobalContext> globalContexts) {
        if (inputsResolved) return;
        inputsResolved = true;

        for (int i = imports.size() - 1; i >= 0; i--) {
            final Import anImport = imports.get(i);
            try {
                anImport.findModule(globalContexts);
            } catch (Exception e) {
                imports.remove(anImport);
                throw e;
            }
        }
    }

    public Value evaluate(ParserNode node) {
        return super.evaluate(node, this, SymbolCreationMode.THROW_IF_NOT_EXISTS, new EvaluationContextLocalInformation(super.getVariables()));
    }

    @Override
    public String toString() {
        return "Context '" + source + "':\n" +
               "  modules: " + modules + "\n" +
               "  imports: " + imports;
    }

    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) return value;
        }
        return null;
    }
}
