package de.yanwittmann.matheval;

import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.operator.Operators;
import de.yanwittmann.matheval.parser.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParserRuleTest {

    @Test
    public void jonasExpressionParseTreeTest() {
        Operators operators = new Operators();
        Assertions.assertEquals(
                "STATEMENT\n" +
                "└─ PARENTHESIS_PAIR\n" +
                "   ├─ EXPRESSION: + (110 l r)\n" +
                "   │  ├─ EXPRESSION: * (120 l r)\n" +
                "   │  │  ├─ NUMBER_LITERAL: 4\n" +
                "   │  │  └─ PARENTHESIS_PAIR\n" +
                "   │  │     └─ EXPRESSION: + (110 l r)\n" +
                "   │  │        ├─ NUMBER_LITERAL: 2\n" +
                "   │  │        └─ NUMBER_LITERAL: 3\n" +
                "   │  └─ FUNCTION_CALL\n" +
                "   │     ├─ IDENTIFIER_ACCESSED\n" +
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
    public void subsequentFunctionCallParseTreeTest() {
        Operators operators = new Operators();
        Assertions.assertEquals(
                "STATEMENT\n" +
                "└─ FUNCTION_CALL\n" +
                "   ├─ IDENTIFIER_ACCESSED\n" +
                "   │  ├─ IDENTIFIER_ACCESSED\n" +
                "   │  │  ├─ IDENTIFIER_ACCESSED\n" +
                "   │  │  │  ├─ IDENTIFIER_ACCESSED\n" +
                "   │  │  │  │  ├─ IDENTIFIER: a\n" +
                "   │  │  │  │  └─ IDENTIFIER: b\n" +
                "   │  │  │  └─ IDENTIFIER_ACCESSED\n" +
                "   │  │  │     ├─ IDENTIFIER: c\n" +
                "   │  │  │     └─ IDENTIFIER: d\n" +
                "   │  │  └─ IDENTIFIER: e\n" +
                "   │  └─ IDENTIFIER_ACCESSED\n" +
                "   │     ├─ IDENTIFIER: f\n" +
                "   │     └─ IDENTIFIER_ACCESSED\n" +
                "   │        ├─ STRING_LITERAL: \"g\"\n" +
                "   │        └─ IDENTIFIER: h\n" +
                "   └─ PARENTHESIS_PAIR\n" +
                "      └─ IDENTIFIER: i",
                new Parser(new Lexer("a[b].c.d[e].f.\"g\".h(i)", operators)).testGetTokenTree()
        );
    }

    @Test
    public void lengthyStatementTest() {
        Operators operators = new Operators();
        Assertions.assertEquals(
                "STATEMENT\n" +
                "└─ ASSIGNMENT: = (10 l r)\n" +
                "   ├─ IDENTIFIER_ACCESSED\n" +
                "   │  ├─ IDENTIFIER_ACCESSED\n" +
                "   │  │  ├─ IDENTIFIER: test\n" +
                "   │  │  └─ EXPRESSION: + (110 l r)\n" +
                "   │  │     ├─ STRING_LITERAL: \"varname-\"\n" +
                "   │  │     └─ IDENTIFIER: test\n" +
                "   │  └─ EXPRESSION: + (110 l r)\n" +
                "   │     ├─ STRING_LITERAL: \"second\"\n" +
                "   │     └─ FUNCTION_CALL\n" +
                "   │        ├─ IDENTIFIER: foo\n" +
                "   │        └─ PARENTHESIS_PAIR\n" +
                "   └─ EXPRESSION: + (110 l r)\n" +
                "      ├─ PARENTHESIS_PAIR\n" +
                "      │  ├─ NUMBER_LITERAL: 3\n" +
                "      │  └─ PARENTHESIS_PAIR\n" +
                "      │     ├─ NUMBER_LITERAL: 5\n" +
                "      │     └─ EXPRESSION: + (110 l r)\n" +
                "      │        ├─ EXPRESSION: * (120 l r)\n" +
                "      │        │  ├─ IDENTIFIER: PI\n" +
                "      │        │  └─ NUMBER_LITERAL: 4\n" +
                "      │        └─ FUNCTION_CALL\n" +
                "      │           ├─ IDENTIFIER: sum\n" +
                "      │           └─ PARENTHESIS_PAIR\n" +
                "      │              └─ FUNCTION_CALL\n" +
                "      │                 ├─ IDENTIFIER: map\n" +
                "      │                 └─ PARENTHESIS_PAIR\n" +
                "      │                    ├─ ARRAY\n" +
                "      │                    │  ├─ NUMBER_LITERAL: 5\n" +
                "      │                    │  ├─ NUMBER_LITERAL: 6\n" +
                "      │                    │  └─ NUMBER_LITERAL: 7\n" +
                "      │                    └─ IDENTIFIER: mapper\n" +
                "      └─ ARRAY\n" +
                "         ├─ NUMBER_LITERAL: 34\n" +
                "         └─ FUNCTION_CALL\n" +
                "            ├─ IDENTIFIER_ACCESSED\n" +
                "            │  ├─ IDENTIFIER: foo\n" +
                "            │  └─ IDENTIFIER: bar\n" +
                "            └─ PARENTHESIS_PAIR\n" +
                "               └─ IDENTIFIER: x",
                new Parser(new Lexer("test[\"varname-\" + test][\"second\" + foo()] = (3, (5, PI * 4 + sum(map([5, 6, 7], mapper)))) + [34, foo.bar(x)]", operators)).testGetTokenTree()
        );
    }

    @Test
    public void currentTest() {
        Operators operators = new Operators();
        // new Parser(new Lexer("1 + 2 * 4 + 6", operators));
        // new Parser(new Lexer("regex = r/regex/ig", operators));
        // new Parser(new Lexer("foo = var + 2 * 4;3 + 6", operators));
        // new Parser(new Lexer("1,2,3,(4,5)", operators));
        // new Parser(new Lexer("(1, 3, (foo, 5), 9 + 8)", operators));
        // new Parser(new Lexer("1 * (3 + 4)", operators));
        // new Parser(new Lexer("foo.bar(1, 2, test(3 + 6) * 4)", operators));

        // new Parser(new Lexer("\"a\".b.c();a.b.c();a[b].c()", operators));
        new Parser(new Lexer("a[b].c.d[e].f.\"g\".h(i)", operators));
    }
}