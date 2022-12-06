package de.yanwittmann.matheval.io;

import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.operator.Operators;
import de.yanwittmann.matheval.parser.Parser;
import de.yanwittmann.matheval.parser.ParserNode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExpressionFileReader {

    private final Lexer lexer;
    private final Parser parser;

    public ExpressionFileReader(Operators operators) {
        lexer = new Lexer(operators);
        parser = new Parser(operators);
    }

    public List<Token> lex(File file) throws IOException {
        final List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        return lexer.parse(lines);
    }

    public ParserNode parse(File file) throws IOException {
        return parser.parse(lex(file));
    }
}
