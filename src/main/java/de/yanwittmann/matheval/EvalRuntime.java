package de.yanwittmann.matheval;

import de.yanwittmann.matheval.interpreter.structure.Context;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.operator.Operators;
import de.yanwittmann.matheval.parser.Parser;
import de.yanwittmann.matheval.parser.ParserNode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EvalRuntime {

    protected final Lexer lexer;
    protected final Parser parser;
    protected final List<Context> contexts = new ArrayList<>();

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

        final Context context = new Context(rootNode, source);
        contexts.add(context);
    }

    public void finish() {
        for (Context context : contexts) {
            context.finish(contexts);
        }
    }

    public Object evaluate(String expression) {
        final List<Token> tokens = lexer.parse(expression);
        final ParserNode tokenTree = parser.parse(tokens);

        final Context context = new Context(tokenTree, "eval");
        context.finish(contexts);
        return context.evaluate();
    }
}
