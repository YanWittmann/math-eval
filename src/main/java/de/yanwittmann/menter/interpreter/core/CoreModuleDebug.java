package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.EvaluationContextLocalInformation;
import de.yanwittmann.menter.interpreter.structure.GlobalContext;
import de.yanwittmann.menter.interpreter.structure.value.CustomType;
import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.lexer.Lexer;
import de.yanwittmann.menter.lexer.Token;

import java.util.ArrayList;
import java.util.List;

public abstract class CoreModuleDebug {

    static {
        EvaluationContext.registerNativeFunction("debug.mtr", "breakFlow", CoreModuleDebug::breakFlow);
        EvaluationContext.registerNativeFunction("debug.mtr", "switch", CoreModuleDebug::debugSwitch);
        EvaluationContext.registerNativeFunction("debug.mtr", "stackTraceValues", CoreModuleDebug::stackTraceValues);
        EvaluationContext.registerNativeFunction("debug.mtr", "explain", CoreModuleDebug::explain);
    }

    /**
     * Executes the function passed as parameter and returns the result. The specialty of this function is that it will
     * also show all steps that were executed to get to the result.
     *
     * @param context          The global context.
     * @param localInformation The local information of the evaluation context.
     * @param arguments        The arguments passed to the function.
     * @return The result of the function.
     */
    public static Value explain(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> arguments) {
        final String[][] parameterCombinations = {
                {PrimitiveValueType.FUNCTION.getType()},
                {PrimitiveValueType.FUNCTION.getType(), PrimitiveValueType.BOOLEAN.getType()},
                {PrimitiveValueType.FUNCTION.getType(), PrimitiveValueType.BOOLEAN.getType(), PrimitiveValueType.BOOLEAN.getType()},
        };
        CustomType.assertAtLeastOneOfParameterCombinationExists("debug.mtr", "explain", arguments, parameterCombinations);

        MenterDebugger.logInterpreterEvaluationStyle = arguments.size() > 1 && arguments.get(1).isTrue() || arguments.size() == 1 ? 2 : 0;
        MenterDebugger.logInterpreterResolveSymbols = arguments.size() > 2 && arguments.get(2).isTrue();

        final Value function = arguments.get(0);
        final Value result = context.evaluateFunction("explain.explain", function, context, localInformation, new ArrayList<>());

        MenterDebugger.logInterpreterEvaluationStyle = 0;
        MenterDebugger.logInterpreterResolveSymbols = false;

        return result;
    }

    public static Value breakFlow(List<Value> arguments) {
        MenterDebugger.haltOnEveryExecutionStep = true;
        return Value.empty();
    }

    public static Value debugSwitch(List<Value> arguments) {
        if (arguments.size() == 1) {
            // switch the debugger flag
            final String debuggerFlag = arguments.get(0).getValue().toString();
            switch (debuggerFlag) {
                case "lexer":
                    MenterDebugger.logLexedTokens = !MenterDebugger.logLexedTokens;
                    break;
                case "parser":
                    MenterDebugger.logParsedTokens = !MenterDebugger.logParsedTokens;
                    break;
                case "parser progress":
                    MenterDebugger.logParseProgress = !MenterDebugger.logParseProgress;
                    break;
                case "interpreter resolve":
                    MenterDebugger.logInterpreterResolveSymbols = !MenterDebugger.logInterpreterResolveSymbols;
                    break;
                case "import order":
                    MenterDebugger.logInterpreterEvaluationOrder = !MenterDebugger.logInterpreterEvaluationOrder;
                    break;

                default:
                    throw new IllegalArgumentException("Unknown debugger flag: " + debuggerFlag + ", must be one of: lexer, parser, parser progress, interpreter resolve, import order");
            }

        } else if (arguments.size() == 2) {
            // set the debugger flag to the value
            final String debuggerFlag = arguments.get(0).getValue().toString();
            boolean isArg1True = arguments.get(1).isTrue();

            switch (debuggerFlag) {
                case "interpreter":
                    MenterDebugger.logInterpreterEvaluationStyle = arguments.get(1).getNumericValue().intValue();
                    break;
                case "lexer":
                    MenterDebugger.logLexedTokens = isArg1True;
                    break;
                case "parser":
                    MenterDebugger.logParsedTokens = isArg1True;
                    break;
                case "parser progress":
                    MenterDebugger.logParseProgress = isArg1True;
                    break;
                case "interpreter resolve":
                    MenterDebugger.logInterpreterResolveSymbols = isArg1True;
                    break;
                case "import order":
                    MenterDebugger.logInterpreterEvaluationOrder = isArg1True;
                    break;

                default:
                    throw new IllegalArgumentException("Unknown debugger flag: " + debuggerFlag + ", must be one of: lexer, parser, parser progress, interpreter resolve, import order");
            }
        }

        return Value.empty();
    }

    public static Value stackTraceValues(List<Value> arguments) {
        MenterDebugger.stackTracePrintValues.clear();
        for (Value argument : arguments) {
            MenterDebugger.stackTracePrintValues.add(argument.getValue().toString());
        }
        return Value.empty();
    }
}