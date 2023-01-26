package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.util.List;

public abstract class CoreModuleDebug {

    static {
        EvaluationContext.registerNativeFunction("debug.mtr", "switch", CoreModuleDebug::debugSwitch);
        EvaluationContext.registerNativeFunction("debug.mtr", "stackTraceValues", CoreModuleDebug::stackTraceValues);
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
                    throw new IllegalArgumentException("Unknown debugger flag: " + debuggerFlag);
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
                    throw new IllegalArgumentException("Unknown debugger flag: " + debuggerFlag);
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