package de.yanwittmann.matheval;

import de.yanwittmann.matheval.interpreter.structure.GlobalContext;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.operator.Operators;
import de.yanwittmann.matheval.parser.Parser;
import de.yanwittmann.matheval.parser.ParserNode;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EvalRuntime {

    private static final Logger LOG = LogManager.getLogger(EvalRuntime.class);

    protected final Lexer lexer;
    protected final Parser parser;
    protected final List<GlobalContext> globalContexts = new ArrayList<>();

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
        final List<Token> tokens = lexer.parse(str);
        final ParserNode rootNode = parser.parse(tokens);

        final GlobalContext globalContext = new GlobalContext(rootNode, source);
        globalContexts.add(globalContext);
    }

    public void finish() {
        for (GlobalContext globalContext : globalContexts) {
            globalContext.resolveImports(globalContexts);
        }
        for (GlobalContext globalContext : globalContexts) {
            globalContext.evaluate();
        }
    }

    public Object evaluate(String expression) {
        final List<Token> tokens = lexer.parse(expression);
        final ParserNode tokenTree = parser.parse(tokens);

        final GlobalContext context = new GlobalContext(tokenTree, "eval");
        context.resolveImports(globalContexts);
        return context.evaluate();
    }
}
