package de.yanwittmann.matheval;

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
                filesToLoad.addAll(FileUtils.listFiles(file, new String[]{"me"}, true));
            } else {
                filesToLoad.add(file);
            }
        }

        for (File file : filesToLoad) {
            try {
                loadContexts(FileUtils.readLines(file, StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadContexts(List<String> str) {
        final List<Token> tokens = lexer.parse(str);
        final ParserNode parserNode = parser.parse(tokens);


    }

    public Object evaluate(String expression) {
        final List<Token> tokens = lexer.parse(expression);
        final ParserNode tokenTree = parser.parse(tokens);


        return null;
    }
}
