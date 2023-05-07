package de.yanwittmann.menter.interpreter.type;

import de.yanwittmann.menter.interpreter.structure.value.*;

import java.math.BigDecimal;
import java.util.List;

@TypeMetaData(typeName = "User", moduleName = "users")
public class User extends CustomType {

    private Value name;
    private Value age;

    public User(List<Value> parameters) {
        super(parameters);

        final String[][] parameterCombinations = {
                {},
                {PrimitiveValueType.STRING.getType(), PrimitiveValueType.NUMBER.getType()}
        };
        final int parameterCombination = CustomType.checkParameterCombination(parameters, parameterCombinations);

        switch (parameterCombination) {
            case 0:
                name = Value.empty();
                age = Value.empty();
                break;
            case 1:
                name = parameters.get(0);
                age = parameters.get(1);
                break;
            case -1:
                throw invalidParameterCombinationException(getClass().getSimpleName(), "User", parameters, parameterCombinations);
        }
    }

    @TypeFunction
    public Value getName(List<Value> parameters) {
        return new Value(name);
    }

    @TypeFunction
    public Value setName(List<Value> parameters) {
        final String[][] parameterCombinations = {
                {},
                {PrimitiveValueType.STRING.getType()}
        };
        final int parameterCombination = CustomType.checkParameterCombination(parameters, parameterCombinations);

        switch (parameterCombination) {
            case 0:
                name = Value.empty();
                break;
            case 1:
                name = parameters.get(0);
                break;
            case -1:
                throw invalidParameterCombinationException(getClass().getSimpleName(), "setName", parameters, parameterCombinations);
        }

        return Value.empty();
    }

    @TypeFunction
    public Value getAge(List<Value> parameters) {
        return new Value(age);
    }

    @TypeFunction
    public Value setAge(List<Value> parameters) {
        final String[][] parameterCombinations = {
                {},
                {PrimitiveValueType.NUMBER.getType()}
        };
        final int parameterCombination = CustomType.checkParameterCombination(parameters, parameterCombinations);

        switch (parameterCombination) {
            case 0:
                age = Value.empty();
                break;
            case 1:
                age = parameters.get(0);
                break;
            case -1:
                throw invalidParameterCombinationException(getClass().getSimpleName(), "setAge", parameters, parameterCombinations);
        }

        return Value.empty();
    }

    @Override
    public String toString() {
        return "User{" +
                "name=" + name.toDisplayString() +
                ", age=" + age.toDisplayString() +
                '}';
    }
}
