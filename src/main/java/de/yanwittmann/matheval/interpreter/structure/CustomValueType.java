package de.yanwittmann.matheval.interpreter.structure;

import java.math.BigDecimal;
import java.util.HashMap;

public interface CustomValueType {

    String getType();

    HashMap<String, MenterValueFunction> getFunctions();

    boolean isType(Object value);

    /**
     * This method is called when a value of this type is accessed via a dot or bracket notation.<br>
     * The identifier is the identifier that was used to access the value.
     *
     * @param thisValue  The value that is accessed.
     * @param identifier The identifier used to access the value.
     * @return The value accessed by the identifier or <code>null</code> if the identifier is not valid/does not exist/the value is not accessible.
     */
    Value accessValue(Value thisValue, Value identifier);

    /**
     * If this method is called, the {@link CustomValueType#accessValue} must have returned <code>null</code> right before.<br>
     * The <code>accessedValue</code> parameter is an empty value that can be filled with whatever content or left empty.
     *
     * @param thisValue         The value that is accessed.
     * @param identifier        The identifier used to access the value.
     * @param accessedValue     The value that was accessed.
     * @param isFinalIdentifier Whether the identifier is the final identifier in the access chain.
     * @return Whether the accessed value has been created by this custom value type.
     */
    boolean createAccessedValue(Value thisValue, Value identifier, Value accessedValue, boolean isFinalIdentifier);

    BigDecimal getNumericValue(Value thisValue);

    boolean isTrue(Value thisValue);

    int size(Value thisValue);

    String toDisplayString(Object thisValue);
}
