package de.yanwittmann.menter;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.ModuleOptions;
import de.yanwittmann.menter.interpreter.structure.GlobalContext;
import de.yanwittmann.menter.interpreter.structure.Import;
import de.yanwittmann.menter.interpreter.structure.Value;
import de.yanwittmann.menter.lexer.Lexer;
import de.yanwittmann.menter.lexer.Token;
import de.yanwittmann.menter.operator.Operators;
import de.yanwittmann.menter.parser.Parser;
import de.yanwittmann.menter.parser.ParserNode;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EvalRuntime {

    private static final Logger LOG = LogManager.getLogger(EvalRuntime.class);

    protected final Lexer lexer;
    protected final Parser parser;
    protected final List<GlobalContext> globalContexts = new ArrayList<>();
    protected final Map<GlobalContext, ParserNode> unfinishedGlobalContextRootObjects = new HashMap<>();
    private final ModuleOptions moduleOptions = new ModuleOptions();

    public EvalRuntime(Operators operators) {
        lexer = new Lexer(operators);
        parser = new Parser(operators);
    }

    public void loadFile(File file) {
        loadFiles(Collections.singletonList(file));
    }

    public void loadFiles(List<File> files) {
        final List<File> filesToLoad = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                filesToLoad.addAll(FileUtils.listFiles(file, new String[]{"ter"}, true));
            } else {
                filesToLoad.add(file);
            }
        }

        for (File file : filesToLoad) {
            try {
                loadContext(FileUtils.readLines(file, StandardCharsets.UTF_8), file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadContext(List<String> str, String source) {
        if (moduleOptions.hasAutoImports()) {
            str.set(0, moduleOptions.getAutoImportsAsString());
        }
        final List<Token> tokens = lexer.parse(str);
        final ParserNode rootNode = parser.parse(tokens);

        final GlobalContext globalContext = new GlobalContext(source);
        globalContext.findImportExportStatements(rootNode, moduleOptions);

        globalContexts.add(globalContext);
        unfinishedGlobalContextRootObjects.put(globalContext, rootNode);
    }

    public void finishLoadingContexts() {
        for (GlobalContext globalContext : globalContexts) {
            globalContext.resolveImports(globalContexts);
        }

        // find the order in which the global contexts have to be executed by using the imports
        final List<GlobalContext> orderedGlobalContexts = new ArrayList<>();
        final List<GlobalContext> globalContextsToCheck = new ArrayList<>(globalContexts);

        int iteration = 0;
        final int maxIterations = globalContexts.size() * 2;
        while (globalContextsToCheck.size() > 0) {
            if (iteration > maxIterations) {
                throw new MenterExecutionException("Circular dependency in imports found. Could not resolve the order of the global contexts.\n" +
                                                   unfinishedGlobalContextRootObjects.keySet().stream()
                                                           .map(GlobalContext::getSource)
                                                           .reduce((s1, s2) -> s1 + "\n" + s2)
                                                           .orElse("No source found."));
            }
            iteration++;

            final GlobalContext globalContext = globalContextsToCheck.get(0);
            if (globalContext.getImports().size() == 0) {
                orderedGlobalContexts.add(globalContext);
                globalContextsToCheck.remove(globalContext);
            } else {
                boolean allImportsLoaded = true;
                for (Import anImport : globalContext.getImports()) {
                    if (!orderedGlobalContexts.contains(anImport.getModule().getParentContext())) {
                        allImportsLoaded = false;
                        break;
                    }
                }
                if (allImportsLoaded) {
                    orderedGlobalContexts.add(globalContext);
                    globalContextsToCheck.remove(globalContext);
                }
            }
        }

        if (MenterDebugger.logInterpreterEvaluationOrder) {
            LOG.info("Determined the following evaluation order for the global contexts:");
            for (GlobalContext globalContext : orderedGlobalContexts) {
                if (unfinishedGlobalContextRootObjects.containsKey(globalContext)) {
                    LOG.info(" - " + globalContext.getSource());
                }
            }
        }

        // execute the global contexts in the correct order
        for (GlobalContext globalContext : orderedGlobalContexts) {
            if (unfinishedGlobalContextRootObjects.containsKey(globalContext)) {
                final ParserNode rootNode = unfinishedGlobalContextRootObjects.get(globalContext);
                globalContext.evaluate(rootNode);
                unfinishedGlobalContextRootObjects.remove(globalContext);
            }
        }
    }

    public Value evaluate(String expression) {
        final List<Token> tokens = lexer.parse(moduleOptions.getAutoImportsAsString() + expression);
        final ParserNode tokenTree = parser.parse(tokens);

        final GlobalContext context = new GlobalContext("eval");
        context.findImportExportStatements(tokenTree, moduleOptions);

        context.resolveImports(globalContexts);
        return context.evaluate(tokenTree);
    }

    public Value evaluateInContextOf(String expression, String contextSource) {
        // attempt to find a context with the given source
        GlobalContext context = globalContexts.stream()
                .filter(globalContext -> globalContext.getSource().equals(contextSource))
                .findFirst()
                .orElse(null);

        // if no context was found, create a new one
        if (context == null) {
            final ParserNode tokenTree;
            if (moduleOptions.hasAutoImports()) {
                final List<Token> tokens = lexer.parse(moduleOptions.getAutoImportsAsString());
                tokenTree = parser.parse(tokens);
            } else {
                tokenTree = new ParserNode(ParserNode.NodeType.ROOT, null);
            }

            context = new GlobalContext(contextSource);
            context.findImportExportStatements(tokenTree, moduleOptions);

            globalContexts.add(context);
        }

        final List<Token> tokens = lexer.parse(expression);
        final ParserNode tokenTree = parser.parse(tokens);

        context.findImportExportStatements(tokenTree, moduleOptions);
        context.resolveImports(globalContexts);

        if (tokenTree.getChildren().size() > 0) {
            return context.evaluate(tokenTree);
        } else {
            return Value.empty();
        }
    }

    public ModuleOptions getModuleOptions() {
        return moduleOptions;
    }
}
