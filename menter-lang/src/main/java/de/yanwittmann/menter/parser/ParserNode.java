package de.yanwittmann.menter.parser;

import de.yanwittmann.menter.lexer.Token;
import de.yanwittmann.menter.operator.Operator;

import java.util.*;

public class ParserNode {

    private final NodeType type;
    private final Object value;
    private final List<Object> children;

    public ParserNode(NodeType type) {
        this(type, null);
    }

    public ParserNode(NodeType type, Object value) {
        this(type, value, new ArrayList<>());
    }

    public ParserNode(NodeType type, Object value, List<Object> children) {
        this.type = type;
        this.value = value;
        this.children = children;
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
        ROOT,
        ASSIGNMENT,
        EXPRESSION, CODE_BLOCK,
        STATEMENT,
        RETURN_STATEMENT,
        IMPORT_STATEMENT, IMPORT_AS_STATEMENT, IMPORT_INLINE_STATEMENT,
        EXPORT_STATEMENT,
        IDENTIFIER_ACCESSED,
        PARENTHESIS_PAIR, SQUARE_BRACKET_PAIR, CURLY_BRACKET_PAIR,
        FUNCTION_DECLARATION, FUNCTION_CALL, FUNCTION_INLINE,
        ARRAY, LISTED_ELEMENTS,
        MAP, MAP_ELEMENT,
        CONDITIONAL, CONDITIONAL_BRANCH, CONDITIONAL_BRACKET,
        LOOP_FOR, LOOP_FOR_BRACKET, LOOP_WHILE,
        CONSTRUCTOR_CALL
    }

    public String reconstructCode() {
        final StringBuilder sb = new StringBuilder();
        reconstructCode(this, sb);
        return sb.toString().trim();
    }

    public static String reconstructCode(Object o) {
        final StringBuilder sb = new StringBuilder();
        reconstructCode(o, sb);
        return sb.toString().trim();
    }

    private static String reconstructCode(Object o, StringBuilder sb) {
        if (o instanceof ParserNode) {
            ParserNode node = (ParserNode) o;
            switch (node.getType()) {
                case ROOT:
                    for (Object child : node.getChildren()) {
                        reconstructCode(child, sb);
                    }
                    break;

                case EXPRESSION:
                    if (node.getValue() instanceof Operator) {
                        Operator operator = (Operator) node.getValue();
                        int i = 0;
                        if (operator.isLeftAssociative()) {
                            reconstructCode(node.getChildren().get(i++), sb);
                            if (operator.isRightAssociative()) {
                                sb.append(" ");
                            }
                        }
                        sb.append(operator.getSymbol());
                        if (operator.isRightAssociative()) {
                            if (operator.isLeftAssociative()) {
                                sb.append(" ");
                            }
                            reconstructCode(node.getChildren().get(i), sb);
                        }
                    } else {
                        for (Object child : node.getChildren()) {
                            reconstructCode(child, sb);
                        }
                    }
                    break;

                case STATEMENT:
                    for (Object child : node.getChildren()) {
                        reconstructCode(child, sb);
                    }
                    sb.append("; ");
                    break;

                case ASSIGNMENT:
                    reconstructCode(node.getChildren().get(0), sb);
                    sb.append(" = ");
                    reconstructCode(node.getChildren().get(1), sb);
                    break;

                case IDENTIFIER_ACCESSED:
                    final Iterator<Object> accessorIterator = node.getChildren().iterator();
                    boolean isFirstAccessor = true;
                    while (accessorIterator.hasNext()) {
                        Object child = accessorIterator.next();

                        boolean wasSpecialCaseAccessor = false;
                        if (child instanceof ParserNode) {
                            ParserNode childNode = (ParserNode) child;
                            if (childNode.getType() == NodeType.CODE_BLOCK) {
                                reconstructCode(new ParserNode(NodeType.ARRAY, null, childNode.getChildren()), sb);
                                wasSpecialCaseAccessor = true;
                            } else if (childNode.getType() == NodeType.FUNCTION_CALL) {
                                reconstructCode(child, sb);
                                wasSpecialCaseAccessor = true;
                            }
                        } else if (child instanceof Token) {
                            Token token = (Token) child;
                            if (Parser.isLiteral(token)) {
                                reconstructCode(new ParserNode(NodeType.ARRAY, null, Collections.singletonList(token.getValue())), sb);
                                wasSpecialCaseAccessor = true;
                            }
                        }

                        if (!wasSpecialCaseAccessor) {
                            if (!isFirstAccessor) {
                                sb.append(".");
                            }
                            reconstructCode(child, sb);
                        }

                        isFirstAccessor = false;
                    }
                    break;

                case PARENTHESIS_PAIR:
                    sb.append("(");
                    boolean isFirstElementOfParenthesisPair = true;
                    for (Object child : node.getChildren()) {
                        if (!isFirstElementOfParenthesisPair) {
                            sb.append(", ");
                        }
                        reconstructCode(child, sb);
                        isFirstElementOfParenthesisPair = false;
                    }
                    sb.append(")");
                    break;

                case SQUARE_BRACKET_PAIR:
                    sb.append("[");
                    for (Object child : node.getChildren()) {
                        reconstructCode(child, sb);
                    }
                    sb.append("]");
                    break;

                case CURLY_BRACKET_PAIR:
                    sb.append("{");
                    for (Object child : node.getChildren()) {
                        reconstructCode(child, sb);
                    }
                    sb.append("}");
                    break;

                case CODE_BLOCK:
                    sb.append("{ ");
                    for (Object child : node.getChildren()) {
                        reconstructCode(child, sb);
                        sb.append("; ");
                    }
                    sb.append("}");
                    break;

                case FUNCTION_DECLARATION:
                    if (Parser.isKeyword(node.getChildren().get(0), "native")) {
                        sb.append("native ");
                        reconstructCode(node.getChildren().get(1), sb);
                        reconstructCode(node.getChildren().get(2), sb);
                    } else {
                        reconstructCode(node.getChildren().get(0), sb);
                        sb.append(" = ");
                        reconstructCode(node.getChildren().get(1), sb);
                        sb.append(" -> ");
                        reconstructCode(node.getChildren().get(2), sb);
                    }
                    break;

                case FUNCTION_CALL:
                    reconstructCode(node.getChildren().get(0), sb);
                    if (node.getChildren().size() > 1) {
                        reconstructCode(node.getChildren().get(1), sb);
                    }
                    break;

                case FUNCTION_INLINE:
                    reconstructCode(node.getChildren().get(0), sb);
                    sb.append(" -> ");
                    reconstructCode(node.getChildren().get(1), sb);
                    break;

                case ARRAY:
                    final Iterator<Object> arrayIterator = node.getChildren().iterator();
                    sb.append("[");
                    while (arrayIterator.hasNext()) {
                        Object child = arrayIterator.next();
                        reconstructCode(child, sb);
                        if (arrayIterator.hasNext()) {
                            sb.append(", ");
                        }
                    }
                    sb.append("]");
                    break;

                case CONDITIONAL:
                    boolean first = true;
                    for (Object child : node.getChildren()) {
                        if (child instanceof ParserNode && ((ParserNode) child).getType() == NodeType.CONDITIONAL_BRANCH && ((ParserNode) child).getChildren().size() == 2) {
                            if (first) {
                                sb.append("if ");
                                first = false;
                            } else {
                                sb.append(" else if ");
                            }
                            reconstructCode(child, sb);
                        } else {
                            sb.append(" else ");
                            reconstructCode(child, sb);
                        }
                    }
                    break;

                case CONDITIONAL_BRANCH:
                    reconstructCode(node.getChildren().get(0), sb);
                    if (node.getChildren().size() == 2) {
                        sb.append(" ");
                        reconstructCode(node.getChildren().get(1), sb);
                    }
                    break;

                case RETURN_STATEMENT:
                    sb.append("return ");
                    for (Object child : node.getChildren()) {
                        reconstructCode(child, sb);
                    }
                    break;

                case IMPORT_STATEMENT:
                case IMPORT_INLINE_STATEMENT:
                case IMPORT_AS_STATEMENT:
                    sb.append("import ");
                    for (Object child : node.getChildren()) {
                        reconstructCode(child, sb);
                    }
                    break;

                case EXPORT_STATEMENT:
                    sb.append("export ");
                    for (Object child : node.getChildren()) {
                        reconstructCode(child, sb);
                    }
                    break;

                case MAP:
                    final Iterator<Object> mapIterator = node.getChildren().iterator();
                    sb.append("{");
                    while (mapIterator.hasNext()) {
                        Object child = mapIterator.next();
                        reconstructCode(child, sb);
                        if (mapIterator.hasNext()) {
                            sb.append(", ");
                        }
                    }
                    sb.append("}");
                    break;

                case MAP_ELEMENT:
                    reconstructCode(node.getChildren().get(0), sb);
                    sb.append(": ");
                    reconstructCode(node.getChildren().get(1), sb);
                    break;

                case LISTED_ELEMENTS:
                    final Iterator<Object> listedIterator = node.getChildren().iterator();
                    while (listedIterator.hasNext()) {
                        Object child = listedIterator.next();
                        reconstructCode(child, sb);
                        if (listedIterator.hasNext()) {
                            sb.append(", ");
                        }
                    }
                    break;

                case LOOP_FOR:
                    sb.append("for (");
                    reconstructCode(node.getChildren().get(0), sb);
                    sb.append(" : ");
                    reconstructCode(node.getChildren().get(1), sb);
                    sb.append(") ");
                    reconstructCode(node.getChildren().get(2), sb);
                    break;

                case CONSTRUCTOR_CALL:
                    sb.append("new ");
                    reconstructCode(node.getChildren(), sb);
                    break;

                default:
                    if (node.getChildren().size() > 0) {
                        for (Object child : node.getChildren()) {
                            reconstructCode(child, sb);
                        }
                    } else {
                        sb.append(node.getValue());
                    }
                    break;
            }
        } else if (o instanceof Token) {
            sb.append(((Token) o).getValue());
        } else if (o instanceof Collection) {
            for (Object child : (Collection) o) {
                reconstructCode(child, sb);
            }
        } else {
            sb.append(o);
        }

        return sb.toString();
    }
}
