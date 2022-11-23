package de.yanwittmann.matheval;

import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.operator.Operators;
import de.yanwittmann.matheval.parser.Parser;
import org.junit.jupiter.api.Test;

class ParserRuleTest {

    @Test
    public void test() {
        Operators operators = new Operators();
        //new Parser(new Lexer("1 + 2 * 4 + 6", operators));
        //new Parser(new Lexer("regex = r/regex/ig", operators));
        //new Parser(new Lexer("foo = var + 2 * 4;3 + 6", operators));
        //new Parser(new Lexer("1,2,3,(4,5)", operators));
        //new Parser(new Lexer("(1, 3, (foo, 5), 9 + 8)", operators));
        new Parser(new Lexer("1 * (3 + 4)", operators));
    }
}