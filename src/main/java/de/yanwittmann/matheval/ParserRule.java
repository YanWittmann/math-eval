package de.yanwittmann.matheval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.yanwittmann.matheval.Lexer.Token;

public class ParserRule {

    private final List<List<Token>> rules;

    public ParserRule() {
        rules = new ArrayList<>();
    }

    public ParserRule(List<Token>... rules) {
        this.rules = new ArrayList<>();
        this.rules.addAll(Arrays.asList(rules));
    }
}
