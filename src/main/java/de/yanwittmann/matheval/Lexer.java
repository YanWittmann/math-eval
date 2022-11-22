package de.yanwittmann.matheval;

import java.util.*;
import java.util.function.Consumer;

public class Lexer {

    private final String expression;
    private final List<Token> tokens = new ArrayList<>();

    public Lexer(String expression) {
        this.expression = expression;

        final TokenIterator iterator = new TokenIterator(expression);
        iterator.forEach(tokens::add);
        System.out.println("\nAll tokens:");
        tokens.forEach(System.out::println);
    }

    public String getExpression() {
        return expression;
    }

    public List<Token> getTokens() {
        return Collections.unmodifiableList(tokens);
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

        public boolean hasNext() {
            return position < string.length();
        }

        public void stepBack() {
            position--;
        }
    }

    public static class Token {
        public final String value;
        public final TokenType type;
        public final int position;

        public Token(String value, TokenType type, int position) {
            this.value = value;
            this.type = type;
            this.position = position;
        }

        @Override
        public String toString() {
            return value + "     (" + type + ")";
        }
    }

    public enum TokenType {
        NUMBER_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL, REGEX_LITERAL, OTHER_LITERAL,
        IDENTIFIER,
        OPERATOR,
        OPEN_PARENTHESIS, CLOSE_PARENTHESIS,
        OPEN_SQUARE_BRACKET, CLOSE_SQUARE_BRACKET,
        OPEN_CURLY_BRACKET, CLOSE_CURLY_BRACKET,
        COMMA, SEMICOLON,
        KEYWORD,
        INDENTED_BLOCK, NEWLINE;

        public Token create(String value, int position) {
            return new Token(value, this, position);
        }
    }

    private static class TokenIterator implements Iterable<Token>, Iterator<Token> {
        private final StringIterator stringIterator;
        private Token nextToken;

        public TokenIterator(String expression) {
            this.stringIterator = new StringIterator(expression + " ");
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
                '+', '-', '*', '/', '%', '^',
                '(', ')', '[', ']', '{', '}',
                ',', ';',
                '!', ':', '.',
                '=', '<', '>', '&', '|'
        };

        private final static String[] DOUBLE_CHARACTER_TOKENS = {
                "==", "!=", "<=", ">=", "&&", "||"
        };

        private final static String[] KEYWORDS = {
                "if", "else", "elif", "true", "false", "null", "export", "as", "import", "inline"
        };

        private boolean isSingleCharacterToken(char c) {
            for (char token : SINGLE_CHARACTER_TOKENS) {
                if (token == c) return true;
            }
            return false;
        }

        private boolean isDoubleCharacterToken(String s) {
            for (String token : DOUBLE_CHARACTER_TOKENS) {
                if (token.equals(s)) return true;
            }
            return false;
        }

        private boolean isKeyword(String s) {
            for (String token : KEYWORDS) {
                if (token.equals(s)) return true;
            }
            return false;
        }

        private void findNext() {
            final StringBuffer buffer = new StringBuffer();
            int state = 0;

            while (this.stringIterator.hasNext()) {
                final char c = this.stringIterator.next();

                switch (state) {
                    case 0:
                        if (c == '\n') {
                            buffer.append(c);
                            if (this.stringIterator.peek() == '\t' || this.stringIterator.peek() == ' ') {
                                buffer.append(this.stringIterator.next());
                                nextToken = createToken(buffer, TokenType.INDENTED_BLOCK);
                            } else {
                                nextToken = createToken(buffer, TokenType.NEWLINE);
                            }
                            return;
                        } else if (Character.isWhitespace(c)) {
                            continue;
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
                        } else if (Character.isLetter(c)) {
                            buffer.append(c);
                            state = 8;
                        } else if (isSingleCharacterToken(c) && !isDoubleCharacterToken("" + c + this.stringIterator.peek())) {
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
                            } else {
                                nextToken = createToken(buffer, TokenType.OPERATOR);
                            }
                            return;
                        } else if (isDoubleCharacterToken("" + c + this.stringIterator.peek())) {
                            buffer.append(c);
                            buffer.append(this.stringIterator.next());
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
                        if (Character.isLetterOrDigit(c) || c == '_') {
                            buffer.append(c);
                        } else {
                            if (isKeyword(buffer.toString())) {
                                final String keyword = buffer.toString();
                                if (keyword.equals("true") || keyword.equals("false")) {
                                    nextToken = createToken(buffer, TokenType.BOOLEAN_LITERAL);
                                } else {
                                    nextToken = createToken(buffer, TokenType.KEYWORD);
                                }
                            } else {
                                nextToken = createToken(buffer, TokenType.IDENTIFIER);
                            }
                            this.stringIterator.stepBack();
                            return;
                        }
                        break;
                    case 9:
                        if (isSingleCharacterToken(c)) {
                            nextToken = createToken(buffer, TokenType.OPERATOR);
                            this.stringIterator.stepBack();
                            return;
                        } else if (isDoubleCharacterToken("" + c + this.stringIterator.peek())) {
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
                }
            }

            nextToken = null;
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
