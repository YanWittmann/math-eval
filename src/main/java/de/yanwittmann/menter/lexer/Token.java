package de.yanwittmann.menter.lexer;

import de.yanwittmann.menter.operator.Operator;

public class Token {
    public final String value;
    public final Lexer.TokenType type;
    public final int position;

    public Token(Lexer.TokenType type, String value, int position) {
        this.value = value;
        this.type = type;
        this.position = position;
    }

    public Token(Lexer.TokenType type, String value) {
        this(type, value, -1);
    }

    public String getValue() {
        return value;
    }

    public Lexer.TokenType getType() {
        return type;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return type + (Operator.isEmpty(value) ? "" : ": " + value);
    }
}
