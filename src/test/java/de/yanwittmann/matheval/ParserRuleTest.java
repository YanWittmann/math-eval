package de.yanwittmann.matheval;

import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.operator.Operators;
import de.yanwittmann.matheval.parser.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParserRuleTest {

    @Test
    public void test() {
        Operators operators = new Operators();
        // new Parser(new Lexer("1 + 2 * 4 + 6", operators));
        // new Parser(new Lexer("regex = r/regex/ig", operators));
        // new Parser(new Lexer("foo = var + 2 * 4;3 + 6", operators));
        // new Parser(new Lexer("1,2,3,(4,5)", operators));
        // new Parser(new Lexer("(1, 3, (foo, 5), 9 + 8)", operators));
        // new Parser(new Lexer("1 * (3 + 4)", operators));
        // new Parser(new Lexer("foo.bar(1, 2, test(3 + 6) * 4)", operators));
        Assertions.assertEquals("STATEMENT\n" +
                                "└─ PARENTHESIS_PAIR\n" +
                                "   ├─ EXPRESSION: + (110 l r)\n" +
                                "   │  ├─ EXPRESSION: * (120 l r)\n" +
                                "   │  │  ├─ NUMBER_LITERAL: 4\n" +
                                "   │  │  └─ PARENTHESIS_PAIR\n" +
                                "   │  │     └─ EXPRESSION: + (110 l r)\n" +
                                "   │  │        ├─ NUMBER_LITERAL: 2\n" +
                                "   │  │        └─ NUMBER_LITERAL: 3\n" +
                                "   │  └─ FUNCTION_CALL\n" +
                                "   │     ├─ ACCESSOR_IDENTIFIER\n" +
                                "   │     │  ├─ IDENTIFIER: foo\n" +
                                "   │     │  └─ IDENTIFIER: bar\n" +
                                "   │     └─ PARENTHESIS_PAIR\n" +
                                "   │        └─ EXPRESSION: + (110 l r)\n" +
                                "   │           ├─ NUMBER_LITERAL: 1\n" +
                                "   │           └─ NUMBER_LITERAL: 2\n" +
                                "   └─ FUNCTION_CALL\n" +
                                "      ├─ IDENTIFIER: hallo\n" +
                                "      └─ PARENTHESIS_PAIR\n" +
                                "         └─ EXPRESSION: + (110 l r)\n" +
                                "            ├─ NUMBER_LITERAL: 3\n" +
                                "            └─ NUMBER_LITERAL: 5\n" +
                                "STATEMENT\n" +
                                "└─ ASSIGNMENT: = (10 l r)\n" +
                                "   ├─ IDENTIFIER: foo\n" +
                                "   └─ FUNCTION_CALL\n" +
                                "      ├─ IDENTIFIER: hallo\n" +
                                "      └─ PARENTHESIS_PAIR\n" +
                                "         └─ EXPRESSION: + (110 l r)\n" +
                                "            ├─ NUMBER_LITERAL: 3\n" +
                                "            └─ NUMBER_LITERAL: 5",
                new Parser(new Lexer("(4 * (2 + 3) + foo.bar(1 + 2), hallo(3 + 5)); foo = hallo(3 + 5)", operators)).testGetTokenTree());
    }

    @Test
    public void currentTest() {
        Operators operators = new Operators();
        new Parser(new Lexer("test[\"varname-\" + test][\"second\" + foo()] = (3, (5, PI * 4 + sum(map([5, 6, 7], mapper)))) + [34, foo.bar(x)]", operators));
        //new Parser(new Lexer("\"d\".toUppercase()", operators));
    }
}