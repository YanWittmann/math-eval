package de.yanwittmann.menter.interpreter.structure.value;

public enum PrimitiveValueType {

    NUMBER("number"),
    STRING("string"),
    BOOLEAN("boolean"),
    OBJECT("object"),
    REGEX("regex"), MATCHER("matcher"),
    FUNCTION("function"),
    VALUE_FUNCTION("value_function"),
    NATIVE_FUNCTION("native_function"),
    REFLECTIVE_FUNCTION("reflective_function"),
    ITERATOR("iterator"),
    @Deprecated
    ARRAY("array"),
    CUSTOM_TYPE("custom_type"),
    ANY("any");

    private final String type;

    PrimitiveValueType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static boolean isType(Value actual, String expected) {
        return isType(actual.getType(), expected);
    }

    public static boolean isType(Value actual, PrimitiveValueType expected) {
        if (actual == null) return false;
        return isType(actual.getType(), expected.getType());
    }

    public static boolean isType(String actual, String expected) {
        if (expected.equals(actual) || expected.equals(ANY.getType()) || actual.equals(ANY.getType())) {
            return true;
        }
        if (expected.equals(FUNCTION.getType()) && actual.endsWith("function")) {
            return true;
        }
        return false;
    }
}
