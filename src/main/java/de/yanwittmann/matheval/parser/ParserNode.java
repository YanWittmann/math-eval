package de.yanwittmann.matheval.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ParserNode {

    private final NodeType type;
    private final Object value;
    private final List<Object> children = new ArrayList<>();

    public ParserNode(NodeType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public NodeType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public List<Object> getChildren() {
        return children;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public void addChild(Object child) {
        children.add(child);
    }

    public void addChild(int index, Object child) {
        children.add(index, child);
    }

    public void addChildren(Collection<Object> children) {
        this.children.addAll(children);
    }

    public void addChildren(Object... children) {
        this.children.addAll(Arrays.asList(children));
    }

    public void removeChild(Object child) {
        children.remove(child);
    }

    /**
     * Formats the node tree below this node.<br>
     * Example:<br>
     * <code>
     * type: value<br>
     * ├─ type: value<br>
     * │  ├─ type: value<br>
     * │  └─ type: value<br>
     * └─ type: value
     * </code>
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, "", "");
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private void toString(StringBuilder sb, String prefix, String childrenPrefix) {
        sb.append(prefix).append(type);
        if (value != null) {
            sb.append(": ").append(value);
        }
        sb.append("\n");

        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            boolean isLast = i == children.size() - 1;

            if (child instanceof ParserNode) {
                ((ParserNode) child).toString(sb, childrenPrefix + (isLast ? "└─ " : "├─ "), childrenPrefix + (isLast ? "   " : "│  "));
            } else {
                sb.append(childrenPrefix).append(isLast ? "└─ " : "├─ ").append(child).append("\n");
            }
        }
    }

    public enum NodeType {
        ASSIGNMENT,
        EXPRESSION, STATEMENT,
        IDENTIFIER, IDENTIFIER_ACCESSED,
        PARENTHESIS_PAIR, SQUARE_BRACKET_PAIR, CURLY_BRACKET_PAIR,
        FUNCTION, FUNCTION_CALL, FUNCTION_CALL_NAME,
        LISTED_ELEMENTS,
        ARRAY, MAP,
    }
}
