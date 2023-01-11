package de.yanwittmann.menter.interpreter.type;

import de.yanwittmann.menter.interpreter.structure.value.*;

import java.util.Iterator;
import java.util.List;

@TypeMetaData(typeName = "customType001", moduleName = "test")
public class Type001 extends CustomType {

    private String myValue = "";

    public Type001(List<Value> parameters) {
        super(parameters);
        setMyValue(parameters);
    }

    @TypeFunction
    public Value getMyValue(List<Value> parameters) {
        return new Value(myValue);
    }

    @TypeFunction
    public Value setMyValue(List<Value> parameters) {
        final int parameterCombination = super.checkParameterCombination(parameters, new String[][]{
                {},
                {PrimitiveValueType.ANY.getType()}
        });

        switch (parameterCombination) {
            case 0:
                myValue = "";
                break;
            case 1:
                myValue = parameters.get(0).toDisplayString();
                break;
        }

        return Value.empty();
    }

    public Value doStuff(List<Value> parameters) {
        System.out.println("Hello World!");

        return Value.empty();
    }

    @Override
    public boolean isTrue() {
        return !myValue.isEmpty();
    }

    @Override
    public Value iterator() {
        return new Value(myValue).iterator();
    }

    @Override
    public String toString() {
        return myValue.isEmpty() ? "(empty)" : myValue;
    }
}
