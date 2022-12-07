package de.yanwittmann.matheval.interpreter.structure;

public enum ValueType {

    NUMBER("number"),
    STRING("string"),
    BOOLEAN("boolean"),
    ARRAY("array"),
    FUNCTION("function"),
    REGEX("regex");

    private final String type;

    ValueType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
