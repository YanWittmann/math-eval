package de.yanwittmann.menter.interpreter.structure;

public enum PrimitiveValueType {

    NUMBER("number"),
    STRING("string"),
    BOOLEAN("boolean"),
    ARRAY("array"),
    FUNCTION("function"),
    VALUE_FUNCTION("value_function"),
    NATIVE_FUNCTION("native_function"),
    OBJECT("object"),
    REGEX("regex"),
    ITERATOR("iterator"),
    ANY("unknown");

    private final String type;

    PrimitiveValueType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
