package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.value.CustomType;
import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.util.List;

public class CoreModuleReflection {

    static {
        EvaluationContext.registerNativeFunction("reflect.mtr", "inherit", CoreModuleReflection::inherit);
        EvaluationContext.registerNativeFunction("reflect.mtr", "access", CoreModuleReflection::access);
    }

    public static Value inherit(List<Value> parameters) {
        final String[][] parameterCombinations = {
                {PrimitiveValueType.ANY.getType(), PrimitiveValueType.ANY.getType()},
        };
        final int parameterCombination = CustomType.checkParameterCombination(parameters, parameterCombinations);

        switch (parameterCombination) {
            case 0:
                parameters.get(0).inheritValue(parameters.get(1));
                break;
            default:
                throw CustomType.invalidParameterCombinationException("reflect", "inherit", parameters, parameterCombinations);
        }

        return parameters.get(0);
    }

    public static Value access(List<Value> parameters) {
        final String[][] parameterCombinations = {
                {PrimitiveValueType.ANY.getType(), PrimitiveValueType.ANY.getType()},
        };
        final int parameterCombination = CustomType.checkParameterCombination(parameters, parameterCombinations);

        switch (parameterCombination) {
            case 0:
                final Value accessed = parameters.get(0).access(parameters.get(1));
                if (accessed == null) return Value.empty();
                else return accessed;
            default:
                throw CustomType.invalidParameterCombinationException("reflect", "access", parameters, parameterCombinations);
        }
    }
}
