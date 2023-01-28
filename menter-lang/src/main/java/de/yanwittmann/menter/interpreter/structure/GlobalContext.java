package de.yanwittmann.menter.interpreter.structure;

import de.yanwittmann.menter.EvalRuntime;
import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.ModuleOptions;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.parser.ParserNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GlobalContext extends EvaluationContext {

    private static final Logger LOG = LogManager.getLogger(GlobalContext.class);

    private final Object source;
    private final List<Module> modules = new ArrayList<>();
    private final List<Import> imports = new ArrayList<>();

    public GlobalContext(Object source) {
        super(null);
        this.source = source;
    }

    public void findImportExportStatements(ParserNode root, ModuleOptions moduleOptions) {
        String exceptionMessage = null;

        for (Object child : root.getChildren()) {
            if (child instanceof ParserNode) {
                final ParserNode childNode = (ParserNode) child;

                if (childNode.getType() == ParserNode.NodeType.EXPORT_STATEMENT) {

                    final Module module = new Module(this, childNode);
                    if (isModuleRegistered(module.getName())) {
                        exceptionMessage = "Duplicate module name: " + module.getName();
                        break;
                    }
                    modules.add(module);

                } else if (childNode.getType() == ParserNode.NodeType.IMPORT_STATEMENT ||
                           childNode.getType() == ParserNode.NodeType.IMPORT_INLINE_STATEMENT ||
                           childNode.getType() == ParserNode.NodeType.IMPORT_AS_STATEMENT) {

                    final Import anImport = new Import(childNode);
                    if (isImportRegistered(anImport.getName())) {
                        exceptionMessage = "Duplicate import name: " + anImport.getName();
                        break;
                    } else if (moduleOptions.isImportForbidden(anImport.getName())) {
                        exceptionMessage = "Import is forbidden: " + anImport.getName();
                        break;
                    }

                    imports.add(anImport);
                    inputsResolved = false;
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

        if (exceptionMessage != null) {
            throw new MenterExecutionException(exceptionMessage);
        }
    }

    private boolean isModuleRegistered(String name) {
        for (Module module : modules) {
            if (module.getName().equals(name)) return true;
        }
        return false;
    }

    private boolean isImportRegistered(String name) {
        for (Import anImport : imports) {
            final String otherName = firstNonNull(anImport.getAlias(), anImport.getName());
            if (name.equals(otherName)) return true;
        }
        return false;
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

    public void resolveImports(EvalRuntime runtime, List<GlobalContext> globalContexts) {
        if (inputsResolved) return;
        inputsResolved = true;

        final List<Import> unresolvedImports = imports.stream()
                .filter(anImport -> anImport.getModule() == null)
                .collect(Collectors.toList());

        for (int i = unresolvedImports.size() - 1; i >= 0; i--) {
            final Import anImport = unresolvedImports.get(i);

            try {
                final List<File> dependingFiles = runtime.findDependingFilesFromImports(Collections.singletonList(anImport.getName()));
                if (dependingFiles.size() > 0) {
                    runtime.loadFiles(dependingFiles);
                }
            } catch (IOException e) {
                throw new MenterExecutionException("Failed to resolve imports", e);
            }

            try {
                final Module customTypeModule = Value.findCustomTypeModule(anImport.getName());
                if (customTypeModule != null) {
                    anImport.setReferencingModule(customTypeModule);
                    continue;
                }

                // otherwise search for a module in the global contexts
                anImport.findModule(globalContexts, runtime.detectAvailableMenterModules().keySet());
            } catch (Exception e) {
                imports.remove(anImport);
                throw e;
            }
        }

        runtime.finishLoadingContexts();
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
