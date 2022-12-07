package de.yanwittmann.matheval.interpreter.structure;

public enum PrimitiveValueType {

    NUMBER("number"),
    STRING("string"),
    BOOLEAN("boolean"),
    ARRAY("array"),
    FUNCTION("function"),
    REGEX("regex");

    private final String type;

    PrimitiveValueType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
