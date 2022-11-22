package de.yanwittmann.matheval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static de.yanwittmann.matheval.Lexer.*;

public class Parser {

    private final Lexer lexer;
    private final List<ParseNode> nodes = new ArrayList<>();

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        parse();
    }

    private final static List<ParserRule> RULES = new ArrayList<>();

    static {
        RULES.add(new ParserRule(
        ));
    }

    private void parse() {
        final List<Token> tokens = lexer.getTokens();

        final Iterator<Token> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            final Token token = iterator.next();

        }
    }
}
