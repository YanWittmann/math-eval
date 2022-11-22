package de.yanwittmann.matheval;

import java.util.ArrayList;
import java.util.List;

public class ParseNode {
    private final Lexer.Token token;
    private final ParseNode parent;
    private final List<ParseNode> children = new ArrayList<>();

    public ParseNode(Lexer.Token token, ParseNode parent) {
        this.token = token;
        this.parent = parent;
    }

    public void addChild(ParseNode child) {
        children.add(child);
    }

    public void removeChild(ParseNode child) {
        children.remove(child);
    }

    public Lexer.Token getToken() {
        return token;
    }

    public ParseNode getParent() {
        return parent;
    }

    public List<ParseNode> getChildren() {
        return children;
    }
}
