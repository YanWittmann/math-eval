package de.yanwittmann.matheval;

import de.yanwittmann.matheval.interpreter.MenterDebugger;
import de.yanwittmann.matheval.io.ExpressionFileReader;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.operator.Operators;
import de.yanwittmann.matheval.parser.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class ParserTest {

    private final static Operators DEFAULT_OPERATORS = new Operators();

    @Test
    @Disabled
    public void customTest() {
        MenterDebugger.logParseProgress = true;
        MenterDebugger.logParsedTokens = true;
        MenterDebugger.logInterpreterEvaluation = true;
        MenterDebugger.logInterpreterResolveSymbols = true;

        // assertParsedTreeEquals("",
        //        "test = (x) -> return x + 1");
    }

    @Test
    public void forLoopsTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ LOOP_FOR\n" +
                               "   ├─ IDENTIFIER: e\n" +
                               "   ├─ IDENTIFIER: list\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ FUNCTION_CALL\n" +
                               "         ├─ IDENTIFIER: print\n" +
                               "         └─ PARENTHESIS_PAIR\n" +
                               "            └─ IDENTIFIER: i",
                "for (e : list) {\n" +
                "    print(i)\n" +
                "}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ LOOP_FOR\n" +
                               "   ├─ ARRAY\n" +
                               "   │  ├─ IDENTIFIER: i\n" +
                               "   │  └─ IDENTIFIER: e\n" +
                               "   ├─ IDENTIFIER: list\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ FUNCTION_CALL\n" +
                               "         ├─ IDENTIFIER: print\n" +
                               "         └─ PARENTHESIS_PAIR\n" +
                               "            └─ IDENTIFIER: i",
                "for ([i, e] : list) {\n" +
                "    print(i)\n" +
                "}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ LOOP_FOR\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  ├─ IDENTIFIER: i\n" +
                               "   │  └─ IDENTIFIER: e\n" +
                               "   ├─ IDENTIFIER: list\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ FUNCTION_CALL\n" +
                               "         ├─ IDENTIFIER: print\n" +
                               "         └─ PARENTHESIS_PAIR\n" +
                               "            └─ IDENTIFIER: i",
                "for ((i, e) : list) {\n" +
                "    print(i)\n" +
                "}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ LOOP_FOR\n" +
                               "   ├─ IDENTIFIER: e\n" +
                               "   ├─ IDENTIFIER: list\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      ├─ IDENTIFIER: print\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         └─ IDENTIFIER: i",
                "for (e : list) print(i)");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ LOOP_FOR\n" +
                               "   ├─ ARRAY\n" +
                               "   │  ├─ IDENTIFIER: i\n" +
                               "   │  └─ IDENTIFIER: e\n" +
                               "   ├─ IDENTIFIER: list\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      ├─ IDENTIFIER: print\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         └─ IDENTIFIER: i",
                "for ([i, e] : list) print(i)");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ LOOP_FOR\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  ├─ IDENTIFIER: i\n" +
                               "   │  └─ IDENTIFIER: e\n" +
                               "   ├─ IDENTIFIER: list\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      ├─ IDENTIFIER: print\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         └─ IDENTIFIER: i",
                "for ((i, e) : list) print(i)");
    }

    @Test
    public void jonasExpressionParseTreeTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ PARENTHESIS_PAIR\n" +
                               "   ├─ IDENTIFIER_ACCESSED\n" +
                               "   │  ├─ IDENTIFIER: foo\n" +
                               "   │  ├─ IDENTIFIER: bar\n" +
                               "   │  └─ FUNCTION_CALL\n" +
                               "   │     └─ PARENTHESIS_PAIR\n" +
                               "   │        └─ EXPRESSION: + (110 l r)\n" +
                               "   │           ├─ NUMBER_LITERAL: 1\n" +
                               "   │           └─ NUMBER_LITERAL: 2\n" +
                               "   └─ NUMBER_LITERAL: 3",
                "(foo.bar(1 + 2), 3)");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ PARENTHESIS_PAIR\n" +
                               "   ├─ EXPRESSION: + (110 l r)\n" +
                               "   │  ├─ EXPRESSION: * (120 l r)\n" +
                               "   │  │  ├─ NUMBER_LITERAL: 4\n" +
                               "   │  │  └─ PARENTHESIS_PAIR\n" +
                               "   │  │     └─ EXPRESSION: + (110 l r)\n" +
                               "   │  │        ├─ NUMBER_LITERAL: 2\n" +
                               "   │  │        └─ NUMBER_LITERAL: 3\n" +
                               "   │  └─ IDENTIFIER_ACCESSED\n" +
                               "   │     ├─ IDENTIFIER: foo\n" +
                               "   │     ├─ IDENTIFIER: bar\n" +
                               "   │     └─ FUNCTION_CALL\n" +
                               "   │        └─ PARENTHESIS_PAIR\n" +
                               "   │           └─ EXPRESSION: + (110 l r)\n" +
                               "   │              ├─ NUMBER_LITERAL: 1\n" +
                               "   │              └─ NUMBER_LITERAL: 2\n" +
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
                "(4 * (2 + 3) + foo.bar(1 + 2), hallo(3 + 5)); foo = hallo(3 + 5)");
    }

    @Test
    public void subsequentFunctionCallParseTreeTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: a\n" +
                               "   ├─ CODE_BLOCK\n" +
                               "   │  └─ IDENTIFIER: b\n" +
                               "   ├─ IDENTIFIER: c\n" +
                               "   ├─ IDENTIFIER: d\n" +
                               "   ├─ CODE_BLOCK\n" +
                               "   │  └─ IDENTIFIER: e\n" +
                               "   ├─ IDENTIFIER: f\n" +
                               "   ├─ CODE_BLOCK\n" +
                               "   │  └─ STRING_LITERAL: \"g\"\n" +
                               "   ├─ IDENTIFIER: h\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         └─ IDENTIFIER: i",
                "a[b].c.d[e].f[\"g\"].h(i)");
    }

    @Test
    public void lengthyStatementTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ASSIGNMENT: = (10 l r)\n" +
                               "   ├─ IDENTIFIER_ACCESSED\n" +
                               "   │  ├─ IDENTIFIER: test\n" +
                               "   │  ├─ CODE_BLOCK\n" +
                               "   │  │  └─ EXPRESSION: + (110 l r)\n" +
                               "   │  │     ├─ STRING_LITERAL: \"varname-\"\n" +
                               "   │  │     └─ IDENTIFIER: test\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     └─ EXPRESSION: + (110 l r)\n" +
                               "   │        ├─ STRING_LITERAL: \"second\"\n" +
                               "   │        └─ FUNCTION_CALL\n" +
                               "   │           ├─ IDENTIFIER: foo\n" +
                               "   │           └─ PARENTHESIS_PAIR\n" +
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
                               "         └─ IDENTIFIER_ACCESSED\n" +
                               "            ├─ IDENTIFIER: foo\n" +
                               "            ├─ IDENTIFIER: bar\n" +
                               "            └─ FUNCTION_CALL\n" +
                               "               └─ PARENTHESIS_PAIR\n" +
                               "                  └─ IDENTIFIER: x",
                "test[\"varname-\" + test][\"second\" + foo()] = (3, (5, PI * 4 + sum(map([5, 6, 7], mapper)))) + [34, foo.bar(x)]");
    }

    @Test
    public void minorStatementsTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ EXPRESSION: + (110 l r)\n" +
                               "   ├─ EXPRESSION: + (110 l r)\n" +
                               "   │  ├─ NUMBER_LITERAL: 1\n" +
                               "   │  └─ EXPRESSION: * (120 l r)\n" +
                               "   │     ├─ NUMBER_LITERAL: 2\n" +
                               "   │     └─ NUMBER_LITERAL: 4\n" +
                               "   └─ NUMBER_LITERAL: 6",
                "1 + 2 * 4 + 6");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ASSIGNMENT: = (10 l r)\n" +
                               "   ├─ IDENTIFIER: regex\n" +
                               "   └─ REGEX_LITERAL: r/regex/ig",
                "regex = r/regex/ig");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ASSIGNMENT: = (10 l r)\n" +
                               "   ├─ IDENTIFIER: foo\n" +
                               "   └─ EXPRESSION: + (110 l r)\n" +
                               "      ├─ IDENTIFIER: var\n" +
                               "      └─ EXPRESSION: * (120 l r)\n" +
                               "         ├─ NUMBER_LITERAL: 2\n" +
                               "         └─ NUMBER_LITERAL: 4\n" +
                               "STATEMENT\n" +
                               "└─ EXPRESSION: + (110 l r)\n" +
                               "   ├─ NUMBER_LITERAL: 3\n" +
                               "   └─ NUMBER_LITERAL: 6",
                "foo = var + 2 * 4;3 + 6");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ PARENTHESIS_PAIR\n" +
                               "   ├─ NUMBER_LITERAL: 1\n" +
                               "   ├─ NUMBER_LITERAL: 2\n" +
                               "   ├─ NUMBER_LITERAL: 3\n" +
                               "   └─ PARENTHESIS_PAIR\n" +
                               "      ├─ NUMBER_LITERAL: 4\n" +
                               "      └─ NUMBER_LITERAL: 5",
                "(1,2,3,(4,5))");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ PARENTHESIS_PAIR\n" +
                               "   ├─ NUMBER_LITERAL: 1\n" +
                               "   ├─ NUMBER_LITERAL: 3\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  ├─ IDENTIFIER: foo\n" +
                               "   │  └─ NUMBER_LITERAL: 5\n" +
                               "   └─ EXPRESSION: + (110 l r)\n" +
                               "      ├─ NUMBER_LITERAL: 9\n" +
                               "      └─ NUMBER_LITERAL: 8",
                "(1, 3, (foo, 5), 9 + 8)");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ EXPRESSION: * (120 l r)\n" +
                               "   ├─ NUMBER_LITERAL: 1\n" +
                               "   └─ PARENTHESIS_PAIR\n" +
                               "      └─ EXPRESSION: + (110 l r)\n" +
                               "         ├─ NUMBER_LITERAL: 3\n" +
                               "         └─ NUMBER_LITERAL: 4",
                "1 * (3 + 4)");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ STRING_LITERAL: \"a\"\n" +
                               "   ├─ IDENTIFIER: b\n" +
                               "   ├─ IDENTIFIER: c\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: a\n" +
                               "   ├─ IDENTIFIER: b\n" +
                               "   ├─ IDENTIFIER: c\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: a\n" +
                               "   ├─ CODE_BLOCK\n" +
                               "   │  └─ IDENTIFIER: b\n" +
                               "   ├─ IDENTIFIER: c\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR",
                "\"a\".b.c();a.b.c();a[b].c()");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ MAP\n" +
                               "   ├─ MAP_ELEMENT\n" +
                               "   │  ├─ IDENTIFIER: test\n" +
                               "   │  └─ IDENTIFIER: hello\n" +
                               "   └─ MAP_ELEMENT\n" +
                               "      ├─ IDENTIFIER: z\n" +
                               "      └─ STRING_LITERAL: \"test\"",
                "{test: hello, z: \"test\"}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ARRAY\n" +
                               "   ├─ NUMBER_LITERAL: 1\n" +
                               "   ├─ NUMBER_LITERAL: 2\n" +
                               "   └─ NUMBER_LITERAL: 3",
                "[1, 2, 3]");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ARRAY\n" +
                               "   ├─ NUMBER_LITERAL: 1\n" +
                               "   ├─ ARRAY\n" +
                               "   │  ├─ NUMBER_LITERAL: 5\n" +
                               "   │  └─ NUMBER_LITERAL: 3\n" +
                               "   └─ NUMBER_LITERAL: 3",
                "[1, [5, 3], 3]");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ARRAY\n" +
                               "   ├─ NUMBER_LITERAL: 34\n" +
                               "   └─ IDENTIFIER_ACCESSED\n" +
                               "      ├─ IDENTIFIER: foo\n" +
                               "      ├─ IDENTIFIER: bar\n" +
                               "      └─ FUNCTION_CALL\n" +
                               "         └─ PARENTHESIS_PAIR\n" +
                               "            └─ IDENTIFIER: x",
                "[34, foo.bar(x)]");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: foo\n" +
                               "   ├─ IDENTIFIER: bar\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         ├─ NUMBER_LITERAL: 1\n" +
                               "         ├─ NUMBER_LITERAL: 2\n" +
                               "         └─ EXPRESSION: * (120 l r)\n" +
                               "            ├─ FUNCTION_CALL\n" +
                               "            │  ├─ IDENTIFIER: test\n" +
                               "            │  └─ PARENTHESIS_PAIR\n" +
                               "            │     └─ EXPRESSION: + (110 l r)\n" +
                               "            │        ├─ NUMBER_LITERAL: 3\n" +
                               "            │        └─ NUMBER_LITERAL: 6\n" +
                               "            └─ NUMBER_LITERAL: 4",
                "foo.bar(1, 2, test(3 + 6) * 4)");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ EXPRESSION: + (110 l r)\n" +
                               "   ├─ NUMBER_LITERAL: 4\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      ├─ IDENTIFIER: sum\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         └─ ARRAY\n" +
                               "            ├─ NUMBER_LITERAL: 5\n" +
                               "            ├─ NUMBER_LITERAL: 6\n" +
                               "            └─ NUMBER_LITERAL: 7",
                "4 + sum([5, 6, 7])");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ PARENTHESIS_PAIR\n" +
                               "   ├─ NUMBER_LITERAL: 5\n" +
                               "   └─ EXPRESSION: + (110 l r)\n" +
                               "      ├─ NUMBER_LITERAL: 4\n" +
                               "      └─ FUNCTION_CALL\n" +
                               "         ├─ IDENTIFIER: map\n" +
                               "         └─ PARENTHESIS_PAIR\n" +
                               "            └─ ARRAY\n" +
                               "               ├─ NUMBER_LITERAL: 5\n" +
                               "               ├─ NUMBER_LITERAL: 6\n" +
                               "               └─ NUMBER_LITERAL: 7",
                "(5, 4 + map([5, 6, 7]))");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ASSIGNMENT: = (10 l r)\n" +
                               "   ├─ LISTED_ELEMENTS\n" +
                               "   │  ├─ IDENTIFIER: un\n" +
                               "   │  └─ IDENTIFIER: pack\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      ├─ IDENTIFIER: foo\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         └─ ARRAY\n" +
                               "            ├─ NUMBER_LITERAL: 1\n" +
                               "            └─ NUMBER_LITERAL: 2",
                "un, pack = foo([1, 2])");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ASSIGNMENT: = (10 l r)\n" +
                               "   ├─ IDENTIFIER: var\n" +
                               "   └─ MAP\n" +
                               "      ├─ MAP_ELEMENT\n" +
                               "      │  ├─ IDENTIFIER: test\n" +
                               "      │  └─ NUMBER_LITERAL: 5\n" +
                               "      ├─ MAP_ELEMENT\n" +
                               "      │  ├─ IDENTIFIER: hmm\n" +
                               "      │  └─ EXPRESSION: * (120 l r)\n" +
                               "      │     ├─ STRING_LITERAL: \"wow\"\n" +
                               "      │     └─ NUMBER_LITERAL: 3\n" +
                               "      └─ MAP_ELEMENT\n" +
                               "         ├─ IDENTIFIER: rec\n" +
                               "         └─ MAP\n" +
                               "            └─ MAP_ELEMENT\n" +
                               "               ├─ IDENTIFIER: arr\n" +
                               "               └─ ARRAY\n" +
                               "                  ├─ NUMBER_LITERAL: 9\n" +
                               "                  └─ NUMBER_LITERAL: 2",
                "var = {test: 5, hmm: \"wow\" * 3, rec: {arr: [9, 2]}}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: TestModule\n" +
                               "   ├─ STRING_LITERAL: \"myFunction\"\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         ├─ IDENTIFIER_ACCESSED\n" +
                               "         │  ├─ IDENTIFIER: TestModule\n" +
                               "         │  └─ CODE_BLOCK\n" +
                               "         │     └─ STRING_LITERAL: \"myVariable\"\n" +
                               "         ├─ NUMBER_LITERAL: 3\n" +
                               "         └─ NUMBER_LITERAL: 6",
                "TestModule[\"myFunction\"](TestModule[\"myVariable\"], 3, 6)");

        assertParsedTreeEquals("IMPORT_STATEMENT\n" +
                               "└─ IDENTIFIER: TestModule",
                "import TestModule");

        assertParsedTreeEquals("IMPORT_AS_STATEMENT\n" +
                               "├─ IDENTIFIER: TestModule\n" +
                               "└─ IDENTIFIER: Test",
                "import TestModule as Test");

        assertParsedTreeEquals("IMPORT_INLINE_STATEMENT\n" +
                               "└─ IDENTIFIER: TestModule",
                "import TestModule inline");

        assertParsedTreeEquals("EXPORT_STATEMENT\n" +
                               "├─ ARRAY\n" +
                               "│  ├─ IDENTIFIER: myFunction\n" +
                               "│  └─ IDENTIFIER: myVariable\n" +
                               "└─ IDENTIFIER: TestModule",
                "export [myFunction, myVariable] as TestModule");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ EXPRESSION: - (110 l r)\n" +
                               "   ├─ FUNCTION_CALL\n" +
                               "   │  ├─ IDENTIFIER: test\n" +
                               "   │  └─ PARENTHESIS_PAIR\n" +
                               "   │     └─ NUMBER_LITERAL: 4\n" +
                               "   └─ NUMBER_LITERAL: 3",
                "test(4) - 3");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ FUNCTION_DECLARATION\n" +
                               "   ├─ IDENTIFIER: myFunction\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  ├─ IDENTIFIER: x\n" +
                               "   │  ├─ IDENTIFIER: y\n" +
                               "   │  └─ IDENTIFIER: z\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ EXPRESSION: - (110 l r)\n" +
                               "         ├─ EXPRESSION: + (110 l r)\n" +
                               "         │  ├─ IDENTIFIER: x\n" +
                               "         │  └─ FUNCTION_CALL\n" +
                               "         │     ├─ IDENTIFIER: pow\n" +
                               "         │     └─ PARENTHESIS_PAIR\n" +
                               "         │        ├─ IDENTIFIER: z\n" +
                               "         │        └─ NUMBER_LITERAL: 2\n" +
                               "         └─ IDENTIFIER: y",
                "myFunction(x, y, z) = x + pow(z, 2)\n - y");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ FUNCTION_DECLARATION\n" +
                               "   ├─ IDENTIFIER: a\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      ├─ FUNCTION_CALL\n" +
                               "      │  ├─ IDENTIFIER: b\n" +
                               "      │  └─ PARENTHESIS_PAIR\n" +
                               "      └─ FUNCTION_CALL\n" +
                               "         ├─ IDENTIFIER: c\n" +
                               "         └─ PARENTHESIS_PAIR",
                "a() = {b(); c()}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ASSIGNMENT: = (10 l r)\n" +
                               "   ├─ IDENTIFIER_ACCESSED\n" +
                               "   │  ├─ IDENTIFIER: fib\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     └─ IDENTIFIER: n\n" +
                               "   └─ EXPRESSION: + (110 l r)\n" +
                               "      ├─ IDENTIFIER_ACCESSED\n" +
                               "      │  ├─ IDENTIFIER: fib\n" +
                               "      │  └─ CODE_BLOCK\n" +
                               "      │     └─ EXPRESSION: - (110 l r)\n" +
                               "      │        ├─ IDENTIFIER: n\n" +
                               "      │        └─ NUMBER_LITERAL: 1\n" +
                               "      └─ IDENTIFIER_ACCESSED\n" +
                               "         ├─ IDENTIFIER: fib\n" +
                               "         └─ CODE_BLOCK\n" +
                               "            └─ EXPRESSION: - (110 l r)\n" +
                               "               ├─ IDENTIFIER: n\n" +
                               "               └─ NUMBER_LITERAL: 2",
                "fib[n] = fib[n - 1] + fib[n - 2];");

        assertParsedTreeEquals("",
                "# this is a comment");

        assertParsedTreeEquals("",
                "##\nmultiline\ncomment\n##");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ FUNCTION_DECLARATION\n" +
                               "   ├─ IDENTIFIER: fibonacci\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  └─ IDENTIFIER: n\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ FUNCTION_CALL\n" +
                               "         ├─ IDENTIFIER: doSomething\n" +
                               "         └─ PARENTHESIS_PAIR",
                "##\n" +
                "function that creates an array of fibonacci numbers\n" +
                "##\n" +
                "fibonacci(n) = {\n" +
                "    doSomething()\n" +
                "}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ FUNCTION_CALL\n" +
                               "   ├─ IDENTIFIER: call\n" +
                               "   └─ PARENTHESIS_PAIR\n" +
                               "STATEMENT\n" +
                               "└─ ASSIGNMENT: = (10 l r)\n" +
                               "   ├─ IDENTIFIER: g\n" +
                               "   └─ EXPRESSION: + (110 l r)\n" +
                               "      ├─ NUMBER_LITERAL: 12\n" +
                               "      └─ NUMBER_LITERAL: 5",
                "call() # this is a comment\ng = 12 + 5");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ FUNCTION_DECLARATION\n" +
                               "   ├─ KEYWORD: native\n" +
                               "   ├─ IDENTIFIER: foo\n" +
                               "   └─ PARENTHESIS_PAIR\n" +
                               "      └─ IDENTIFIER: bar",
                "native foo(bar)");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ FUNCTION_DECLARATION\n" +
                               "   ├─ IDENTIFIER: foo\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  └─ IDENTIFIER: bar\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ RETURN_STATEMENT\n" +
                               "         └─ NUMBER_LITERAL: 4",
                "foo(bar) = return 4");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ FUNCTION_DECLARATION\n" +
                               "   ├─ IDENTIFIER: foo\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  └─ IDENTIFIER: bar\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      ├─ ASSIGNMENT: = (10 l r)\n" +
                               "      │  ├─ IDENTIFIER: test\n" +
                               "      │  └─ NUMBER_LITERAL: 4\n" +
                               "      └─ RETURN_STATEMENT\n" +
                               "         └─ IDENTIFIER: test",
                "foo(bar) = {test = 4; return test}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ ASSIGNMENT: = (10 l r)\n" +
                               "   ├─ IDENTIFIER: test\n" +
                               "   └─ STRING_LITERAL: \"\"hey\"\"",
                "test = \"\\\"hey\\\"\"");
    }

    @Test
    public void conditionTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ CONDITIONAL\n" +
                               "   └─ CONDITIONAL_BRANCH\n" +
                               "      ├─ PARENTHESIS_PAIR\n" +
                               "      │  └─ IDENTIFIER_ACCESSED\n" +
                               "      │     ├─ IDENTIFIER: fibstorage\n" +
                               "      │     ├─ IDENTIFIER: containsKey\n" +
                               "      │     └─ FUNCTION_CALL\n" +
                               "      │        └─ PARENTHESIS_PAIR\n" +
                               "      │           └─ IDENTIFIER: n\n" +
                               "      └─ CODE_BLOCK\n" +
                               "         └─ IDENTIFIER_ACCESSED\n" +
                               "            ├─ IDENTIFIER: fibstorage\n" +
                               "            └─ CODE_BLOCK\n" +
                               "               └─ IDENTIFIER: n",
                "if (fibstorage.containsKey(n)) fibstorage[n];");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ CONDITIONAL\n" +
                               "   ├─ CONDITIONAL_BRANCH\n" +
                               "   │  ├─ PARENTHESIS_PAIR\n" +
                               "   │  │  └─ EXPRESSION: == (80 l r)\n" +
                               "   │  │     ├─ NUMBER_LITERAL: 1\n" +
                               "   │  │     └─ NUMBER_LITERAL: 2\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     └─ CONDITIONAL\n" +
                               "   │        ├─ CONDITIONAL_BRANCH\n" +
                               "   │        │  ├─ PARENTHESIS_PAIR\n" +
                               "   │        │  │  └─ EXPRESSION: < (90 l r)\n" +
                               "   │        │  │     ├─ NUMBER_LITERAL: 3\n" +
                               "   │        │  │     └─ NUMBER_LITERAL: 4\n" +
                               "   │        │  └─ NUMBER_LITERAL: 5\n" +
                               "   │        └─ CONDITIONAL_BRANCH\n" +
                               "   │           └─ NUMBER_LITERAL: 6\n" +
                               "   └─ CONDITIONAL_BRANCH\n" +
                               "      └─ NUMBER_LITERAL: 7",
                "if (1 == 2) if (3 < 4) 5 else 6 else 7");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ CONDITIONAL\n" +
                               "   ├─ CONDITIONAL_BRANCH\n" +
                               "   │  ├─ PARENTHESIS_PAIR\n" +
                               "   │  │  └─ IDENTIFIER: cond1\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     └─ RETURN_STATEMENT\n" +
                               "   │        └─ NUMBER_LITERAL: 4\n" +
                               "   ├─ CONDITIONAL_BRANCH\n" +
                               "   │  ├─ PARENTHESIS_PAIR\n" +
                               "   │  │  └─ IDENTIFIER: cond2\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     ├─ ASSIGNMENT: = (10 l r)\n" +
                               "   │     │  ├─ IDENTIFIER: foo\n" +
                               "   │     │  └─ NUMBER_LITERAL: 5\n" +
                               "   │     └─ RETURN_STATEMENT\n" +
                               "   │        └─ IDENTIFIER: foo\n" +
                               "   └─ CONDITIONAL_BRANCH\n" +
                               "      └─ CODE_BLOCK\n" +
                               "         └─ RETURN_STATEMENT\n" +
                               "            └─ NUMBER_LITERAL: 6",
                "if (cond1) {return 4} elif (cond2) {foo = 5;return foo;} else {return 6;}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ CONDITIONAL\n" +
                               "   ├─ CONDITIONAL_BRANCH\n" +
                               "   │  ├─ PARENTHESIS_PAIR\n" +
                               "   │  │  └─ IDENTIFIER: cond1\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     └─ RETURN_STATEMENT\n" +
                               "   │        └─ NUMBER_LITERAL: 4\n" +
                               "   ├─ CONDITIONAL_BRANCH\n" +
                               "   │  ├─ PARENTHESIS_PAIR\n" +
                               "   │  │  └─ IDENTIFIER: cond2\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     ├─ ASSIGNMENT: = (10 l r)\n" +
                               "   │     │  ├─ IDENTIFIER: foo\n" +
                               "   │     │  └─ NUMBER_LITERAL: 5\n" +
                               "   │     └─ RETURN_STATEMENT\n" +
                               "   │        └─ IDENTIFIER: foo\n" +
                               "   ├─ CONDITIONAL_BRANCH\n" +
                               "   │  ├─ PARENTHESIS_PAIR\n" +
                               "   │  │  └─ IDENTIFIER: cond3\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     └─ IDENTIFIER: test\n" +
                               "   └─ CONDITIONAL_BRANCH\n" +
                               "      └─ CODE_BLOCK\n" +
                               "         └─ RETURN_STATEMENT\n" +
                               "            └─ NUMBER_LITERAL: 6",
                "if (cond1) {\n" +
                "    return 4\n" +
                "} elif (cond2) {" +
                "    foo = 5;\n" +
                "    return foo;\n" +
                "} elif (cond3) {\n" +
                "    test;\n" +
                "} else {\n" +
                "    return 6;\n" +
                "}");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ CONDITIONAL\n" +
                               "   ├─ CONDITIONAL_BRANCH\n" +
                               "   │  ├─ PARENTHESIS_PAIR\n" +
                               "   │  │  └─ EXPRESSION: == (80 l r)\n" +
                               "   │  │     ├─ EXPRESSION: + (110 l r)\n" +
                               "   │  │     │  ├─ IDENTIFIER: var\n" +
                               "   │  │     │  └─ NUMBER_LITERAL: 5\n" +
                               "   │  │     └─ NUMBER_LITERAL: 4\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     └─ CONDITIONAL\n" +
                               "   │        ├─ CONDITIONAL_BRANCH\n" +
                               "   │        │  ├─ PARENTHESIS_PAIR\n" +
                               "   │        │  │  └─ EXPRESSION: > (90 l r)\n" +
                               "   │        │  │     ├─ FUNCTION_CALL\n" +
                               "   │        │  │     │  ├─ IDENTIFIER: foo\n" +
                               "   │        │  │     │  └─ PARENTHESIS_PAIR\n" +
                               "   │        │  │     │     ├─ NUMBER_LITERAL: 4\n" +
                               "   │        │  │     │     └─ ARRAY\n" +
                               "   │        │  │     │        ├─ NUMBER_LITERAL: 4\n" +
                               "   │        │  │     │        └─ NUMBER_LITERAL: 2\n" +
                               "   │        │  │     └─ NUMBER_LITERAL: 4\n" +
                               "   │        │  └─ CODE_BLOCK\n" +
                               "   │        │     └─ FUNCTION_CALL\n" +
                               "   │        │        ├─ IDENTIFIER: bar\n" +
                               "   │        │        └─ PARENTHESIS_PAIR\n" +
                               "   │        └─ CONDITIONAL_BRANCH\n" +
                               "   │           └─ CODE_BLOCK\n" +
                               "   │              └─ ASSIGNMENT: = (10 l r)\n" +
                               "   │                 ├─ IDENTIFIER: test\n" +
                               "   │                 └─ EXPRESSION: + (110 l r)\n" +
                               "   │                    ├─ NUMBER_LITERAL: 5\n" +
                               "   │                    └─ NUMBER_LITERAL: 3\n" +
                               "   ├─ CONDITIONAL_BRANCH\n" +
                               "   │  ├─ PARENTHESIS_PAIR\n" +
                               "   │  │  └─ EXPRESSION: % (120 l r)\n" +
                               "   │  │     ├─ FUNCTION_CALL\n" +
                               "   │  │     │  ├─ IDENTIFIER: var\n" +
                               "   │  │     │  └─ PARENTHESIS_PAIR\n" +
                               "   │  │     │     ├─ NUMBER_LITERAL: 34\n" +
                               "   │  │     │     └─ IDENTIFIER_ACCESSED\n" +
                               "   │  │     │        ├─ IDENTIFIER: foo\n" +
                               "   │  │     │        └─ CODE_BLOCK\n" +
                               "   │  │     │           └─ STRING_LITERAL: \"acc\"\n" +
                               "   │  │     └─ EXPRESSION: == (80 l r)\n" +
                               "   │  │        ├─ NUMBER_LITERAL: 3\n" +
                               "   │  │        └─ NUMBER_LITERAL: 1\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     └─ ASSIGNMENT: = (10 l r)\n" +
                               "   │        ├─ IDENTIFIER: test\n" +
                               "   │        └─ CONDITIONAL\n" +
                               "   │           ├─ CONDITIONAL_BRANCH\n" +
                               "   │           │  ├─ PARENTHESIS_PAIR\n" +
                               "   │           │  │  └─ EXPRESSION: == (80 l r)\n" +
                               "   │           │  │     ├─ FUNCTION_CALL\n" +
                               "   │           │  │     │  ├─ IDENTIFIER: foo\n" +
                               "   │           │  │     │  └─ PARENTHESIS_PAIR\n" +
                               "   │           │  │     │     └─ IDENTIFIER: bar\n" +
                               "   │           │  │     └─ ARRAY\n" +
                               "   │           │  │        └─ IDENTIFIER_ACCESSED\n" +
                               "   │           │  │           ├─ IDENTIFIER: bar\n" +
                               "   │           │  │           └─ CODE_BLOCK\n" +
                               "   │           │  │              └─ IDENTIFIER: foo\n" +
                               "   │           │  └─ CODE_BLOCK\n" +
                               "   │           │     └─ RETURN_STATEMENT\n" +
                               "   │           │        └─ NUMBER_LITERAL: 3\n" +
                               "   │           └─ CONDITIONAL_BRANCH\n" +
                               "   │              └─ CODE_BLOCK\n" +
                               "   │                 └─ RETURN_STATEMENT\n" +
                               "   │                    └─ NUMBER_LITERAL: 6\n" +
                               "   └─ CONDITIONAL_BRANCH\n" +
                               "      └─ CODE_BLOCK\n" +
                               "         └─ ASSIGNMENT: = (10 l r)\n" +
                               "            ├─ IDENTIFIER: test\n" +
                               "            └─ NUMBER_LITERAL: 6",
                "if (var + 5 == 4) {\n" +
                "    if (foo(4, [4, 2]) > 4) {\n" +
                "        bar();\n" +
                "    } else {\n" +
                "        test = 5 + 3;\n" +
                "    }\n" +
                "} elif (var(34, foo[\"acc\"]) % 3 == 1) {\n" +
                "    test = if (foo(bar) == [bar[foo]]) {return 3} else {return 6};\n" +
                "} else {\n" +
                "    test = 6;\n" +
                "}");
    }

    @Test
    public void accessFunctionTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: test\n" +
                               "   ├─ IDENTIFIER: keys\n" +
                               "   ├─ FUNCTION_CALL\n" +
                               "   │  └─ PARENTHESIS_PAIR\n" +
                               "   ├─ IDENTIFIER: size\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR",
                "test.keys().size()");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  └─ IDENTIFIER_ACCESSED\n" +
                               "   │     ├─ IDENTIFIER: test\n" +
                               "   │     ├─ IDENTIFIER: keys\n" +
                               "   │     └─ FUNCTION_CALL\n" +
                               "   │        └─ PARENTHESIS_PAIR\n" +
                               "   ├─ IDENTIFIER: size\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR",
                "(test.keys()).size()");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: test\n" +
                               "   ├─ IDENTIFIER: t\n" +
                               "   ├─ NUMBER_LITERAL: 0\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         └─ NUMBER_LITERAL: 2",
                "test.t[0](2);");
    }

    @Test
    public void inlineFunctionTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ FUNCTION_INLINE: -> (0 l r)\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  └─ IDENTIFIER: x\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ EXPRESSION: + (110 l r)\n" +
                               "         ├─ IDENTIFIER: x\n" +
                               "         └─ NUMBER_LITERAL: 1\n" +
                               "STATEMENT\n" +
                               "└─ FUNCTION_INLINE: -> (0 l r)\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  └─ IDENTIFIER: x\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ RETURN_STATEMENT\n" +
                               "         └─ EXPRESSION: + (110 l r)\n" +
                               "            ├─ IDENTIFIER: x\n" +
                               "            └─ NUMBER_LITERAL: 1\n" +
                               "STATEMENT\n" +
                               "└─ FUNCTION_INLINE: -> (0 l r)\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  └─ IDENTIFIER: x\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ EXPRESSION: + (110 l r)\n" +
                               "         ├─ IDENTIFIER: x\n" +
                               "         └─ NUMBER_LITERAL: 1\n" +
                               "STATEMENT\n" +
                               "└─ FUNCTION_INLINE: -> (0 l r)\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  └─ IDENTIFIER: x\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ RETURN_STATEMENT\n" +
                               "         └─ EXPRESSION: + (110 l r)\n" +
                               "            ├─ IDENTIFIER: x\n" +
                               "            └─ NUMBER_LITERAL: 1",
                "(x) -> x + 1\n" +
                "(x) -> { return x + 1; }\n" +
                "x -> x + 1\n" +
                "x -> { return x + 1; }");
    }

    @Test
    public void newAccessorTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: fibstorage\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ IDENTIFIER: n",
                "fibstorage[n]");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ ARRAY\n" +
                               "   │  └─ IDENTIFIER: test\n" +
                               "   ├─ IDENTIFIER: foo\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR\n" +
                               "         └─ IDENTIFIER: bar",
                "[test].foo(bar)");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: keys\n" +
                               "   ├─ FUNCTION_CALL\n" +
                               "   │  └─ PARENTHESIS_PAIR\n" +
                               "   │     ├─ NUMBER_LITERAL: 1\n" +
                               "   │     └─ IDENTIFIER_ACCESSED\n" +
                               "   │        ├─ IDENTIFIER: test\n" +
                               "   │        ├─ IDENTIFIER: foo\n" +
                               "   │        └─ FUNCTION_CALL\n" +
                               "   │           └─ PARENTHESIS_PAIR\n" +
                               "   │              └─ IDENTIFIER: bar\n" +
                               "   ├─ IDENTIFIER: size\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR",
                "keys(1, test.foo(bar)).size()");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ MAP\n" +
                               "   │  ├─ MAP_ELEMENT\n" +
                               "   │  │  ├─ NUMBER_LITERAL: 1\n" +
                               "   │  │  └─ ARRAY\n" +
                               "   │  │     ├─ NUMBER_LITERAL: 2\n" +
                               "   │  │     └─ NUMBER_LITERAL: 4\n" +
                               "   │  └─ MAP_ELEMENT\n" +
                               "   │     ├─ NUMBER_LITERAL: 2\n" +
                               "   │     └─ NUMBER_LITERAL: 3\n" +
                               "   ├─ IDENTIFIER: keys\n" +
                               "   ├─ FUNCTION_CALL\n" +
                               "   │  └─ PARENTHESIS_PAIR\n" +
                               "   ├─ IDENTIFIER: size\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR",
                "{1: [2, 4], 2: 3}.keys().size()");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: test\n" +
                               "   ├─ IDENTIFIER: keys\n" +
                               "   ├─ FUNCTION_CALL\n" +
                               "   │  └─ PARENTHESIS_PAIR\n" +
                               "   ├─ IDENTIFIER: size\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR",
                "test.keys().size();");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ FUNCTION_INLINE: -> (0 l r)\n" +
                               "   ├─ PARENTHESIS_PAIR\n" +
                               "   │  └─ IDENTIFIER: x\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ IDENTIFIER_ACCESSED\n" +
                               "         ├─ MAP\n" +
                               "         │  ├─ MAP_ELEMENT\n" +
                               "         │  │  ├─ IDENTIFIER: x\n" +
                               "         │  │  └─ IDENTIFIER: x\n" +
                               "         │  └─ MAP_ELEMENT\n" +
                               "         │     ├─ IDENTIFIER: y\n" +
                               "         │     └─ NUMBER_LITERAL: 23\n" +
                               "         ├─ IDENTIFIER: keys\n" +
                               "         ├─ FUNCTION_CALL\n" +
                               "         │  └─ PARENTHESIS_PAIR\n" +
                               "         │     └─ IDENTIFIER: mapper\n" +
                               "         ├─ IDENTIFIER: collect\n" +
                               "         ├─ FUNCTION_CALL\n" +
                               "         │  └─ PARENTHESIS_PAIR\n" +
                               "         │     └─ FUNCTION_CALL\n" +
                               "         │        ├─ IDENTIFIER: toList\n" +
                               "         │        └─ PARENTHESIS_PAIR\n" +
                               "         ├─ IDENTIFIER: attr\n" +
                               "         ├─ IDENTIFIER: size\n" +
                               "         └─ FUNCTION_CALL\n" +
                               "            └─ PARENTHESIS_PAIR",
                "x -> {x: x, y: 23}.keys(mapper).collect(toList()).attr.size()");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: test\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ NUMBER_LITERAL: 3",
                "test[3]");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ EXPRESSION: * (120 l r)\n" +
                               "   ├─ IDENTIFIER_ACCESSED\n" +
                               "   │  ├─ IDENTIFIER: test\n" +
                               "   │  └─ CODE_BLOCK\n" +
                               "   │     └─ NUMBER_LITERAL: 3\n" +
                               "   └─ NUMBER_LITERAL: 2",
                "test[3] * 2");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: test\n" +
                               "   ├─ CODE_BLOCK\n" +
                               "   │  └─ EXPRESSION: + (110 l r)\n" +
                               "   │     ├─ EXPRESSION: + (110 l r)\n" +
                               "   │     │  ├─ NUMBER_LITERAL: 1\n" +
                               "   │     │  └─ NUMBER_LITERAL: 2\n" +
                               "   │     └─ IDENTIFIER_ACCESSED\n" +
                               "   │        ├─ ARRAY\n" +
                               "   │        │  ├─ NUMBER_LITERAL: 3\n" +
                               "   │        │  └─ NUMBER_LITERAL: 4\n" +
                               "   │        └─ IDENTIFIER: foo\n" +
                               "   ├─ IDENTIFIER: bar\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR",
                "test[1 + 2 + [3, 4].foo].bar()");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ EXPRESSION: + (110 l r)\n" +
                               "   ├─ EXPRESSION: * (120 l r)\n" +
                               "   │  ├─ NUMBER_LITERAL: 1\n" +
                               "   │  └─ IDENTIFIER_ACCESSED\n" +
                               "   │     ├─ IDENTIFIER: test\n" +
                               "   │     ├─ CODE_BLOCK\n" +
                               "   │     │  └─ EXPRESSION: + (110 l r)\n" +
                               "   │     │     ├─ EXPRESSION: + (110 l r)\n" +
                               "   │     │     │  ├─ NUMBER_LITERAL: 1\n" +
                               "   │     │     │  └─ NUMBER_LITERAL: 2\n" +
                               "   │     │     └─ IDENTIFIER_ACCESSED\n" +
                               "   │     │        ├─ ARRAY\n" +
                               "   │     │        │  ├─ NUMBER_LITERAL: 3\n" +
                               "   │     │        │  └─ NUMBER_LITERAL: 4\n" +
                               "   │     │        └─ IDENTIFIER: foo\n" +
                               "   │     ├─ IDENTIFIER: bar\n" +
                               "   │     └─ FUNCTION_CALL\n" +
                               "   │        └─ PARENTHESIS_PAIR\n" +
                               "   └─ NUMBER_LITERAL: 2",
                "1 * test[1 + 2 + [3, 4].foo].bar() + 2");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: test\n" +
                               "   ├─ IDENTIFIER: call\n" +
                               "   ├─ FUNCTION_CALL\n" +
                               "   │  └─ PARENTHESIS_PAIR\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ NUMBER_LITERAL: 7",
                "test.call()[7]");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ IDENTIFIER: test\n" +
                               "   ├─ IDENTIFIER: call\n" +
                               "   ├─ FUNCTION_CALL\n" +
                               "   │  └─ PARENTHESIS_PAIR\n" +
                               "   ├─ IDENTIFIER: call\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR",
                "test.call().call()");

        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ ARRAY\n" +
                               "   │  └─ FUNCTION_CALL\n" +
                               "   │     ├─ IDENTIFIER: mapper\n" +
                               "   │     └─ PARENTHESIS_PAIR\n" +
                               "   │        ├─ NUMBER_LITERAL: 1\n" +
                               "   │        └─ NUMBER_LITERAL: 2\n" +
                               "   ├─ IDENTIFIER: keys\n" +
                               "   └─ FUNCTION_CALL\n" +
                               "      └─ PARENTHESIS_PAIR",
                "[mapper(1, 2)].keys()");
    }

    @Test
    public void accessorNightmareTest() {
        assertParsedTreeEquals("STATEMENT\n" +
                               "└─ IDENTIFIER_ACCESSED\n" +
                               "   ├─ MAP\n" +
                               "   │  └─ MAP_ELEMENT\n" +
                               "   │     ├─ NUMBER_LITERAL: 1\n" +
                               "   │     └─ EXPRESSION: * (120 l r)\n" +
                               "   │        ├─ NUMBER_LITERAL: 2\n" +
                               "   │        └─ IDENTIFIER_ACCESSED\n" +
                               "   │           ├─ IDENTIFIER: core\n" +
                               "   │           ├─ IDENTIFIER: systemprop\n" +
                               "   │           └─ FUNCTION_CALL\n" +
                               "   │              └─ PARENTHESIS_PAIR\n" +
                               "   │                 └─ EXPRESSION: * (120 l r)\n" +
                               "   │                    ├─ NUMBER_LITERAL: 4\n" +
                               "   │                    └─ IDENTIFIER_ACCESSED\n" +
                               "   │                       ├─ STRING_LITERAL: \"key\"\n" +
                               "   │                       ├─ IDENTIFIER: lower\n" +
                               "   │                       ├─ FUNCTION_CALL\n" +
                               "   │                       │  └─ PARENTHESIS_PAIR\n" +
                               "   │                       ├─ IDENTIFIER: do\n" +
                               "   │                       ├─ FUNCTION_CALL\n" +
                               "   │                       │  └─ PARENTHESIS_PAIR\n" +
                               "   │                       │     └─ EXPRESSION: * (120 l r)\n" +
                               "   │                       │        ├─ MAP\n" +
                               "   │                       │        │  ├─ MAP_ELEMENT\n" +
                               "   │                       │        │  │  ├─ IDENTIFIER: myMap\n" +
                               "   │                       │        │  │  └─ NUMBER_LITERAL: 3\n" +
                               "   │                       │        │  └─ MAP_ELEMENT\n" +
                               "   │                       │        │     ├─ NUMBER_LITERAL: 4\n" +
                               "   │                       │        │     └─ IDENTIFIER_ACCESSED\n" +
                               "   │                       │        │        ├─ IDENTIFIER: foo\n" +
                               "   │                       │        │        └─ CODE_BLOCK\n" +
                               "   │                       │        │           └─ NUMBER_LITERAL: 0\n" +
                               "   │                       │        └─ NUMBER_LITERAL: 2\n" +
                               "   │                       ├─ IDENTIFIER: replace\n" +
                               "   │                       └─ FUNCTION_CALL\n" +
                               "   │                          └─ PARENTHESIS_PAIR\n" +
                               "   │                             ├─ STRING_LITERAL: \" \"\n" +
                               "   │                             └─ STRING_LITERAL: \"_\"\n" +
                               "   ├─ IDENTIFIER: call\n" +
                               "   ├─ FUNCTION_CALL\n" +
                               "   │  └─ PARENTHESIS_PAIR\n" +
                               "   │     └─ EXPRESSION: * (120 l r)\n" +
                               "   │        ├─ STRING_LITERAL: \"key\"\n" +
                               "   │        └─ IDENTIFIER_ACCESSED\n" +
                               "   │           ├─ ARRAY\n" +
                               "   │           │  ├─ NUMBER_LITERAL: 2\n" +
                               "   │           │  └─ NUMBER_LITERAL: 3\n" +
                               "   │           ├─ IDENTIFIER: size\n" +
                               "   │           └─ FUNCTION_CALL\n" +
                               "   │              └─ PARENTHESIS_PAIR\n" +
                               "   └─ CODE_BLOCK\n" +
                               "      └─ NUMBER_LITERAL: 7",
                "{1: 2 * core.systemprop(4 * \"key\".lower().do({myMap: 3, 4: foo[0]} * 2).replace(\" \", \"_\"))}.call(\"key\" * [2, 3].size())[7]");
    }

    @Test
    public void parseFileTest() throws IOException {
        Parser parser = new Parser(ParserTest.DEFAULT_OPERATORS);
        ExpressionFileReader reader = new ExpressionFileReader(DEFAULT_OPERATORS);

        Assertions.assertEquals("STATEMENT\n" +
                                "└─ FUNCTION_DECLARATION\n" +
                                "   ├─ IDENTIFIER: fibonacci\n" +
                                "   ├─ PARENTHESIS_PAIR\n" +
                                "   │  └─ IDENTIFIER: n\n" +
                                "   └─ CODE_BLOCK\n" +
                                "      └─ CONDITIONAL\n" +
                                "         ├─ CONDITIONAL_BRANCH\n" +
                                "         │  ├─ PARENTHESIS_PAIR\n" +
                                "         │  │  └─ EXPRESSION: == (80 l r)\n" +
                                "         │  │     ├─ IDENTIFIER: n\n" +
                                "         │  │     └─ NUMBER_LITERAL: 0\n" +
                                "         │  └─ CODE_BLOCK\n" +
                                "         │     └─ RETURN_STATEMENT\n" +
                                "         │        └─ ARRAY\n" +
                                "         │           └─ NUMBER_LITERAL: 0\n" +
                                "         ├─ CONDITIONAL_BRANCH\n" +
                                "         │  ├─ PARENTHESIS_PAIR\n" +
                                "         │  │  └─ EXPRESSION: == (80 l r)\n" +
                                "         │  │     ├─ IDENTIFIER: n\n" +
                                "         │  │     └─ NUMBER_LITERAL: 1\n" +
                                "         │  └─ CODE_BLOCK\n" +
                                "         │     └─ RETURN_STATEMENT\n" +
                                "         │        └─ ARRAY\n" +
                                "         │           ├─ NUMBER_LITERAL: 0\n" +
                                "         │           └─ NUMBER_LITERAL: 1\n" +
                                "         └─ CONDITIONAL_BRANCH\n" +
                                "            └─ CODE_BLOCK\n" +
                                "               ├─ ASSIGNMENT: = (10 l r)\n" +
                                "               │  ├─ IDENTIFIER: fib\n" +
                                "               │  └─ FUNCTION_CALL\n" +
                                "               │     ├─ IDENTIFIER: fibonacci\n" +
                                "               │     └─ PARENTHESIS_PAIR\n" +
                                "               │        └─ EXPRESSION: - (110 l r)\n" +
                                "               │           ├─ IDENTIFIER: n\n" +
                                "               │           └─ NUMBER_LITERAL: 1\n" +
                                "               ├─ ASSIGNMENT: = (10 l r)\n" +
                                "               │  ├─ IDENTIFIER_ACCESSED\n" +
                                "               │  │  ├─ IDENTIFIER: fib\n" +
                                "               │  │  └─ CODE_BLOCK\n" +
                                "               │  │     └─ IDENTIFIER: n\n" +
                                "               │  └─ EXPRESSION: + (110 l r)\n" +
                                "               │     ├─ IDENTIFIER_ACCESSED\n" +
                                "               │     │  ├─ IDENTIFIER: fib\n" +
                                "               │     │  └─ CODE_BLOCK\n" +
                                "               │     │     └─ EXPRESSION: - (110 l r)\n" +
                                "               │     │        ├─ IDENTIFIER: n\n" +
                                "               │     │        └─ NUMBER_LITERAL: 1\n" +
                                "               │     └─ IDENTIFIER_ACCESSED\n" +
                                "               │        ├─ IDENTIFIER: fib\n" +
                                "               │        └─ CODE_BLOCK\n" +
                                "               │           └─ EXPRESSION: - (110 l r)\n" +
                                "               │              ├─ IDENTIFIER: n\n" +
                                "               │              └─ NUMBER_LITERAL: 2\n" +
                                "               └─ RETURN_STATEMENT\n" +
                                "                  └─ IDENTIFIER: fib",
                parser.toString(reader.parse(new File("src/test/resources/lang/other/fib.ter")).getChildren()));

        Assertions.assertEquals(
                "STATEMENT\n" +
                "└─ ASSIGNMENT: = (10 l r)\n" +
                "   ├─ IDENTIFIER: test\n" +
                "   └─ FUNCTION_INLINE: -> (0 l r)\n" +
                "      ├─ PARENTHESIS_PAIR\n" +
                "      │  └─ IDENTIFIER: x\n" +
                "      └─ CODE_BLOCK\n" +
                "         └─ RETURN_STATEMENT\n" +
                "            └─ EXPRESSION: + (110 l r)\n" +
                "               ├─ IDENTIFIER: x\n" +
                "               └─ NUMBER_LITERAL: 1\n" +
                "STATEMENT\n" +
                "└─ ASSIGNMENT: = (10 l r)\n" +
                "   ├─ IDENTIFIER: test\n" +
                "   └─ FUNCTION_INLINE: -> (0 l r)\n" +
                "      ├─ PARENTHESIS_PAIR\n" +
                "      │  └─ IDENTIFIER: x\n" +
                "      └─ CODE_BLOCK\n" +
                "         └─ RETURN_STATEMENT\n" +
                "            └─ EXPRESSION: + (110 l r)\n" +
                "               ├─ IDENTIFIER: x\n" +
                "               └─ NUMBER_LITERAL: 1\n" +
                "STATEMENT\n" +
                "└─ ASSIGNMENT: = (10 l r)\n" +
                "   ├─ IDENTIFIER: test\n" +
                "   └─ FUNCTION_INLINE: -> (0 l r)\n" +
                "      ├─ PARENTHESIS_PAIR\n" +
                "      │  └─ IDENTIFIER: x\n" +
                "      └─ CODE_BLOCK\n" +
                "         └─ RETURN_STATEMENT\n" +
                "            └─ EXPRESSION: + (110 l r)\n" +
                "               ├─ IDENTIFIER: x\n" +
                "               └─ NUMBER_LITERAL: 1\n" +
                "STATEMENT\n" +
                "└─ FUNCTION_DECLARATION\n" +
                "   ├─ IDENTIFIER: test\n" +
                "   ├─ PARENTHESIS_PAIR\n" +
                "   │  └─ IDENTIFIER: x\n" +
                "   └─ CODE_BLOCK\n" +
                "      └─ RETURN_STATEMENT\n" +
                "         └─ EXPRESSION: + (110 l r)\n" +
                "            ├─ IDENTIFIER: x\n" +
                "            └─ NUMBER_LITERAL: 1\n" +
                "STATEMENT\n" +
                "└─ FUNCTION_DECLARATION\n" +
                "   ├─ IDENTIFIER: test\n" +
                "   ├─ PARENTHESIS_PAIR\n" +
                "   │  └─ IDENTIFIER: x\n" +
                "   └─ CODE_BLOCK\n" +
                "      └─ RETURN_STATEMENT\n" +
                "         └─ EXPRESSION: + (110 l r)\n" +
                "            ├─ IDENTIFIER: x\n" +
                "            └─ NUMBER_LITERAL: 1",
                parser.toString(reader.parse(new File("src/test/resources/lang/other/inlineFunctions.ter")).getChildren()));
    }

    private void assertParsedTreeEquals(String expected, String expression) {
        Parser parser = new Parser(ParserTest.DEFAULT_OPERATORS);
        Lexer lexer = new Lexer(ParserTest.DEFAULT_OPERATORS);
        Assertions.assertEquals(
                expected,
                parser.toString(parser.parse(lexer.parse(expression)).getChildren()
                ));
    }
}