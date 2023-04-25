package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.interpreter.structure.Module;
import de.yanwittmann.menter.interpreter.structure.*;
import de.yanwittmann.menter.interpreter.structure.value.CustomType;
import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.lexer.Lexer;
import de.yanwittmann.menter.lexer.Token;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CoreModuleReflection {

    static {
        EvaluationContext.registerNativeFunction("reflect.mtr", "inherit", CoreModuleReflection::inherit);
        EvaluationContext.registerNativeFunction("reflect.mtr", "access", CoreModuleReflection::access);

        EvaluationContext.registerNativeFunction("reflect.mtr", "setVariable", CoreModuleReflection::setVariable);
        EvaluationContext.registerNativeFunction("reflect.mtr", "getVariable", CoreModuleReflection::getVariable);
        EvaluationContext.registerNativeFunction("reflect.mtr", "removeVariable", CoreModuleReflection::removeVariable);

        EvaluationContext.registerNativeFunction("reflect.mtr", "getContextName", CoreModuleReflection::getContextName);
        EvaluationContext.registerNativeFunction("reflect.mtr", "getImports", CoreModuleReflection::getImports);
        EvaluationContext.registerNativeFunction("reflect.mtr", "getModules", CoreModuleReflection::getModules);
        EvaluationContext.registerNativeFunction("reflect.mtr", "getVariables", CoreModuleReflection::getVariables);

        EvaluationContext.registerNativeFunction("reflect.mtr", "callFunctionByName", CoreModuleReflection::callFunctionByName);
        EvaluationContext.registerNativeFunction("reflect.mtr", "getStackTrace", CoreModuleReflection::getStackTrace);
        EvaluationContext.registerNativeFunction("reflect.mtr", "printStackTrace", CoreModuleReflection::printStackTrace);
    }

    public static Value inherit(List<Value> parameters) {
        return anyAnyAction(parameters, "inherit", () -> {
            parameters.get(0).inheritValue(parameters.get(1));
            return parameters.get(0);
        });
    }

    public static Value access(List<Value> parameters) {
        return anyAnyAction(parameters, "access", () -> {
            final Value accessed = parameters.get(0).access(parameters.get(1));
            if (accessed == null) return Value.empty();
            else return accessed;
        });
    }

    public static Value setVariable(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        return anyAnyAction(parameters, "setVariable", () -> {
            context.addVariable(parameters.get(0).toDisplayString(), parameters.get(1));
            return Value.empty();
        });
    }

    public static Value getVariable(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        return anyAction(parameters, "getVariable", () -> context.getVariable(parameters.get(0).toDisplayString()));
    }

    public static Value removeVariable(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        return anyAction(parameters, "removeVariable", () -> {
            context.removeVariable(parameters.get(0).toDisplayString());
            return Value.empty();
        });
    }

    public static Value getContextName(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        return new Value(context.getSourceName());
    }

    public static Value getImports(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        final Map<String, Value> imports = new LinkedHashMap<>();
        for (Import anImport : context.getImports()) {
            final Map<String, Value> importMap = new LinkedHashMap<>();
            for (Object symbol : anImport.getModule().getSymbols()) {
                if (symbol instanceof Token) {
                    final Token token = (Token) symbol;
                    final String name = token.getValue();
                    final Value variable = anImport.getModule().getParentContext().getVariable(name);
                    if (variable != null) importMap.put(name, variable);
                }
            }
            imports.put(anImport.getModule().getName(), new Value(importMap));
        }
        return new Value(imports);
    }

    public static Value getModules(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        return new Value(context.getModules().stream().map(Module::getName).collect(Collectors.toList()));
    }

    public static Value getVariables(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        return new Value(context.getVariables());
    }

    public static Value callFunctionByName(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        final String[][] parameterCombinations = {
                {PrimitiveValueType.STRING.getType(), PrimitiveValueType.OBJECT.getType()},
        };
        final int parameterCombination = CustomType.checkParameterCombination(parameters, parameterCombinations);

        if (parameterCombination == 0) {
            final String functionName = parameters.get(0).toDisplayString();
            return context.evaluateFunction(functionName, context.evaluate(new Token(Lexer.TokenType.IDENTIFIER, functionName)), context, localInformation, new ArrayList<>(parameters.get(1).getMap().values()));
        }
        throw CustomType.invalidParameterCombinationException("reflect", "callFunctionByName", parameters, parameterCombinations);
    }

    public static Value getStackTrace(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        return new Value(localInformation.getStackTrace().stream().map(MenterStackTraceElement::getFunctionName).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public static Value printStackTrace(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters) {
        localInformation.printStackTrace(parameters.size() > 0 ? parameters.get(0).toDisplayString() : "Stack trace:");
        return Value.empty();
    }

    private static Value anyAnyAction(List<Value> parameters, String action, Supplier<Value> actionSupplier) {
        final String[][] parameterCombinations = {
                {PrimitiveValueType.ANY.getType(), PrimitiveValueType.ANY.getType()},
        };
        final int parameterCombination = CustomType.checkParameterCombination(parameters, parameterCombinations);

        if (parameterCombination == 0) {
            return actionSupplier.get();
        }
        throw CustomType.invalidParameterCombinationException("reflect", action, parameters, parameterCombinations);
    }

    private static Value anyAction(List<Value> parameters, String action, Supplier<Value> actionSupplier) {
        final String[][] parameterCombinations = {
                {PrimitiveValueType.ANY.getType()},
        };
        final int parameterCombination = CustomType.checkParameterCombination(parameters, parameterCombinations);

        if (parameterCombination == 0) {
            return actionSupplier.get();
        }
        throw CustomType.invalidParameterCombinationException("reflect", action, parameters, parameterCombinations);
    }
}
