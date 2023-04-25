package de.yanwittmann.menter.lexer;

import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.operator.Operators;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class Lexer {

    private static final Logger LOG = LogManager.getLogger(Lexer.class);

    private final Operators operators;

    public Lexer(Operators operators) {
        this.operators = operators;
    }

    public List<Token> parse(String expression) {
        List<Token> tokens = new ArrayList<>();

        new TokenIterator(expression, operators).forEach(tokens::add);
        tokens.add(new Token(TokenType.EOF, "", expression.length()));

        if (MenterDebugger.logLexedTokens) {
            LOG.info("Lexed tokens:");
            for (Token token : tokens) {
                LOG.info(token);
            }
            LOG.info("");
        }

        return tokens;
    }

    public List<Token> parse(List<String> expressions) {
        return parse(String.join("\n", expressions));
    }

    public Operators getOperators() {
        return operators;
    }

    private static class StringIterator {
        private final String string;
        private int position = 0;

        public StringIterator(String string) {
            this.string = string;
        }

        public char next() {
            return string.charAt(position++);
        }

        public char peek() {
            return string.charAt(position);
        }

        public char peek(int offset) {
            if (position + offset >= string.length()) return '\0';
            return string.charAt(position + offset);
        }

        public boolean hasNext() {
            return position < string.length();
        }

        public void stepBack() {
            position--;
        }
    }

    public enum TokenType {
        NUMBER_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL, REGEX_LITERAL, OTHER_LITERAL,
        IDENTIFIER,
        OPERATOR,
        OPEN_PARENTHESIS, CLOSE_PARENTHESIS,
        OPEN_SQUARE_BRACKET, CLOSE_SQUARE_BRACKET,
        OPEN_CURLY_BRACKET, CLOSE_CURLY_BRACKET,
        COMMA, SEMICOLON, DOT,
        CONTINUE, BREAK, PASS,
        KEYWORD,
        NEWLINE,
        COMMENT,
        EOF;

        public Token create(String value, int position) {
            return new Token(this, value, position);
        }
    }

    private static class TokenIterator implements Iterable<Token>, Iterator<Token> {
        private final StringIterator stringIterator;
        private final Operators operators;
        private Token nextToken;
        int lastIndentation = 0;

        public TokenIterator(String expression, Operators operators) {
            this.stringIterator = new StringIterator(expression + " ");
            this.operators = operators;
            findNext();
        }

        @Override
        public Token next() {
            final Token result = nextToken != null ? nextToken : null;
            findNext();
            return result;
        }

        @Override
        public boolean hasNext() {
            return nextToken != null;
        }

        private final static char[] SINGLE_CHARACTER_TOKENS = {
                '(', ')', '[', ']', '{', '}',
                ',', ';',
                ':', '.',
        };

        private final static String[] KEYWORDS = {
                "if", "else", "elif", "true", "false", "null", "export", "as", "import", "inline", "native", "return",
                "while", "for", "break", "continue", "in", "new", "instanceof", "pass", "null"
        };

        private boolean isSingleCharacterToken(char c) {
            for (char token : SINGLE_CHARACTER_TOKENS) {
                if (token == c) return true;
            }
            return false;
        }

        private boolean isOperator(String s) {
            return this.operators.getOperators().stream()
                    .anyMatch(operator -> operator.getSymbol().equals(s));
        }

        private boolean isKeyword(String s) {
            for (String token : KEYWORDS) {
                if (token.equals(s)) return true;
            }
            return false;
        }

        private boolean isIdentifierCharacter(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$';
        }

        private void findNext() {
            final StringBuffer buffer = new StringBuffer();
            int state = 0;

            while (this.stringIterator.hasNext()) {
                final char c = this.stringIterator.next();

                if (c == '\\') {
                    final char next = this.stringIterator.next();
                    if (next == 'n') {
                        buffer.append('\n');
                    } else if (next == 'r') {
                        buffer.append('\r');
                    } else if (next == 't') {
                        buffer.append('\t');
                    } else if (next == 'b') {
                        buffer.append('\b');
                    } else if (next == 'f') {
                        buffer.append('\f');
                    } else if (next == 'u') {
                        final char[] unicode = new char[4];
                        for (int i = 0; i < 4; i++) {
                            unicode[i] = this.stringIterator.next();
                        }
                        buffer.append((char) Integer.parseInt(new String(unicode), 16));
                    } else {
                        buffer.append(next);
                    }
                    continue;
                }

                switch (state) {
                    case 0:
                        if (c == '\n') {
                            buffer.append(c);
                            if (getIndentationValue(this.stringIterator.peek()) > 0) {
                                int indentationCount = 0;
                                int currentIndentationValue;
                                do {
                                    currentIndentationValue = getIndentationValue(this.stringIterator.peek());
                                    indentationCount += currentIndentationValue;
                                    if (currentIndentationValue > 0) this.stringIterator.next();
                                } while (currentIndentationValue > 0 && this.stringIterator.hasNext());

                                if (indentationCount > lastIndentation) {
                                    buffer.setLength(0);
                                    lastIndentation = indentationCount;

                                } else {
                                    nextToken = createToken(buffer, TokenType.NEWLINE);
                                    lastIndentation = indentationCount;
                                    return;
                                }

                                continue;
                            } else {
                                nextToken = createToken(buffer, TokenType.NEWLINE);
                                lastIndentation = 0;
                                return;
                            }
                        } else if (Character.isWhitespace(c)) {
                            continue;
                        } else if (c == '#') {
                            buffer.append(c);
                            state = 12;
                        } else if (c == '0') {
                            buffer.append(c);
                            state = 3;
                        } else if (Character.isDigit(c)) {
                            buffer.append(c);
                            state = 1;
                        } else if (c == '.' && Character.isDigit(this.stringIterator.peek())) {
                            buffer.append(c);
                            state = 2;
                        } else if (c == '"') {
                            buffer.append(c);
                            state = 7;
                        } else if (c == 'r' && this.stringIterator.peek() == '/') {
                            buffer.append(c);
                            buffer.append(this.stringIterator.next());
                            state = 10;
                        } else if (isIdentifierCharacter(c)) {
                            buffer.append(c);
                            state = 8;
                        } else if (isSingleCharacterToken(c) &&
                                   (!isOperator(String.valueOf(c) + this.stringIterator.peek()) && !isOperator(String.valueOf(c) + this.stringIterator.peek() + this.stringIterator.peek(1)))) {
                            buffer.append(c);
                            if (c == '(') {
                                nextToken = createToken(buffer, TokenType.OPEN_PARENTHESIS);
                            } else if (c == ')') {
                                nextToken = createToken(buffer, TokenType.CLOSE_PARENTHESIS);
                            } else if (c == '[') {
                                nextToken = createToken(buffer, TokenType.OPEN_SQUARE_BRACKET);
                            } else if (c == ']') {
                                nextToken = createToken(buffer, TokenType.CLOSE_SQUARE_BRACKET);
                            } else if (c == '{') {
                                nextToken = createToken(buffer, TokenType.OPEN_CURLY_BRACKET);
                            } else if (c == '}') {
                                nextToken = createToken(buffer, TokenType.CLOSE_CURLY_BRACKET);
                            } else if (c == ',') {
                                nextToken = createToken(buffer, TokenType.COMMA);
                            } else if (c == ';') {
                                nextToken = createToken(buffer, TokenType.SEMICOLON);
                            } else if (c == '.') {
                                nextToken = createToken(buffer, TokenType.DOT);
                            } else {
                                nextToken = createToken(buffer, TokenType.OPERATOR);
                            }
                            return;
                        } else if (isOperator(String.valueOf(c) + this.stringIterator.peek() + this.stringIterator.peek(1))) {
                            buffer.append(c);
                            buffer.append(this.stringIterator.next());
                            buffer.append(this.stringIterator.next());
                            nextToken = createToken(buffer, TokenType.OPERATOR);
                            return;
                        } else if (isOperator(String.valueOf(c) + this.stringIterator.peek())) {
                            buffer.append(c);
                            buffer.append(this.stringIterator.next());
                            nextToken = createToken(buffer, TokenType.OPERATOR);
                            return;
                        } else if (isOperator(String.valueOf(c))) {
                            buffer.append(c);
                            nextToken = createToken(buffer, TokenType.OPERATOR);
                            return;
                        } else {
                            buffer.append(c);
                            state = 9;
                        }
                        break;
                    case 1:
                        if (Character.isDigit(c)) {
                            buffer.append(c);
                        } else if (c == '.') {
                            buffer.append(c);
                            state = 2;
                        } else {
                            nextToken = createToken(buffer, TokenType.NUMBER_LITERAL);
                            this.stringIterator.stepBack();
                            return;
                        }
                        break;
                    case 2:
                        if (Character.isDigit(c)) {
                            buffer.append(c);
                        } else {
                            nextToken = createToken(buffer, TokenType.NUMBER_LITERAL);
                            this.stringIterator.stepBack();
                            return;
                        }
                        break;
                    case 3:
                        if (c == 'x') {
                            buffer.append(c);
                            state = 4;
                        } else if (c == 'b') {
                            buffer.append(c);
                            state = 5;
                        } else if (c == 'o') {
                            buffer.append(c);
                            state = 6;
                        } else if (Character.isDigit(c)) {
                            buffer.append(c);
                            state = 1;
                        } else if (c == '.') {
                            buffer.append(c);
                            state = 2;
                        } else {
                            nextToken = createToken(buffer, TokenType.NUMBER_LITERAL);
                            this.stringIterator.stepBack();
                            return;
                        }
                        break;
                    case 4:
                        if (Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                            buffer.append(c);
                        } else {
                            nextToken = createToken(buffer, TokenType.NUMBER_LITERAL);
                            this.stringIterator.stepBack();
                            return;
                        }
                        break;
                    case 5:
                        if (c == '0' || c == '1') {
                            buffer.append(c);
                        } else {
                            nextToken = createToken(buffer, TokenType.NUMBER_LITERAL);
                            this.stringIterator.stepBack();
                            return;
                        }
                        break;
                    case 6:
                        if (c >= '0' && c <= '7') {
                            buffer.append(c);
                        } else {
                            nextToken = createToken(buffer, TokenType.NUMBER_LITERAL);
                            this.stringIterator.stepBack();
                            return;
                        }
                        break;
                    case 7:
                        if (c == '"') {
                            buffer.append(c);
                            nextToken = createToken(buffer, TokenType.STRING_LITERAL);
                            return;
                        } else {
                            buffer.append(c);
                        }
                        break;
                    case 8:
                        if (isIdentifierCharacter(c)) {
                            buffer.append(c);
                        } else {
                            if (isKeyword(buffer.toString())) {
                                final String keyword = buffer.toString();
                                switch (keyword) {
                                    case "true":
                                    case "false":
                                        nextToken = createToken(buffer, TokenType.BOOLEAN_LITERAL);
                                        break;
                                    case "pass":
                                        nextToken = createToken(buffer, TokenType.PASS);
                                        break;
                                    case "break":
                                        nextToken = createToken(buffer, TokenType.BREAK);
                                        break;
                                    case "continue":
                                        nextToken = createToken(buffer, TokenType.CONTINUE);
                                        break;
                                    default:
                                        nextToken = createToken(buffer, TokenType.KEYWORD);
                                        break;
                                }
                            } else {
                                nextToken = createToken(buffer, TokenType.IDENTIFIER);
                            }
                            this.stringIterator.stepBack();
                            return;
                        }
                        break;
                    case 9:
                        if (isSingleCharacterToken(c) || isOperator(String.valueOf(c))) {
                            nextToken = createToken(buffer, TokenType.OPERATOR);
                            this.stringIterator.stepBack();
                            return;
                        } else if (isOperator(String.valueOf(c) + this.stringIterator.peek())) {
                            nextToken = createToken(buffer, TokenType.OPERATOR);
                            this.stringIterator.stepBack();
                            return;
                        } else {
                            buffer.append(c);
                        }
                    case 10: // regex
                        if (c == '/') {
                            buffer.append(c);
                            final char peek = this.stringIterator.peek();
                            if (peek == 'g' || peek == 'i' || peek == 'm') {
                                state = 11;
                            } else {
                                nextToken = createToken(buffer, TokenType.REGEX_LITERAL);
                                return;
                            }
                        } else {
                            buffer.append(c);
                        }
                        break;
                    case 11: // regex flags
                        if (c == 'g' || c == 'i' || c == 'm') {
                            buffer.append(c);
                        } else {
                            nextToken = createToken(buffer, TokenType.REGEX_LITERAL);
                            this.stringIterator.stepBack();
                            return;
                        }
                        break;
                    case 12: // comment start
                        buffer.append(c);
                        if (c == '#') {
                            state = 13; // multiline comment ##
                        } else {
                            state = 16; // single line comment #
                        }
                        break;
                    case 13: // multiline comment
                        buffer.append(c);
                        if (c == '#') {
                            state = 15; // potential comment end
                        } else {
                            state = 14; // comment body
                        }
                        break;
                    case 14: // multiline comment body
                        buffer.append(c);
                        if (c == '#') {
                            state = 15; // potential comment end
                        }
                        break;
                    case 15: // potential multiline comment end
                        buffer.append(c);
                        if (c == '#') {
                            nextToken = createToken(buffer, TokenType.COMMENT);
                            return;
                        } else {
                            state = 14; // return to comment body
                        }
                        break;
                    case 16: // single line comment
                        if (c == '\n' || c == '\r' || !this.stringIterator.hasNext()) {
                            nextToken = createToken(buffer, TokenType.COMMENT);
                            this.stringIterator.stepBack();
                            return;
                        } else {
                            buffer.append(c);
                        }

                }
            }

            nextToken = null;
        }

        private int getIndentationValue(char c) {
            if (c == '\t') {
                return 4;
            } else if (c == ' ') {
                return 1;
            }
            return 0;
        }

        private Token createToken(StringBuffer buffer, TokenType type) {
            return type.create(buffer.toString(), this.stringIterator.position - buffer.length());
        }

        private void unexpectedCharacter(char c) {
            throw new RuntimeException("Unexpected character: \"" + c + "\" around position " + this.stringIterator.position);
        }

        @Override
        public Iterator<Token> iterator() {
            return new Iterator<Token>() {
                @Override
                public boolean hasNext() {
                    return TokenIterator.this.hasNext();
                }

                @Override
                public Token next() {
                    return TokenIterator.this.next();
                }
            };
        }

        @Override
        public void forEach(Consumer<? super Token> action) {
            Iterable.super.forEach(action);
        }

        @Override
        public Spliterator<Token> spliterator() {
            return Iterable.super.spliterator();
        }
    }
}
