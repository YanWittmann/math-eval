package de.yanwittmann.matheval.interpreter.core;

import de.yanwittmann.matheval.interpreter.MenterDebugger;
import de.yanwittmann.matheval.interpreter.structure.Value;

public abstract class CoreModuleDebug {
    public static Value debugSwitch(Value[] arguments) {
        if (arguments.length == 1) {
            // switch the debugger flag
            final String debuggerFlag = arguments[0].getValue().toString();
            switch (debuggerFlag) {
                case "interpreter":
                    MenterDebugger.logInterpreterEvaluation = !MenterDebugger.logInterpreterEvaluation;
                    break;
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

        } else if (arguments.length == 2) {
            // set the debugger flag to the value
            final String debuggerFlag = arguments[0].getValue().toString();
            switch (debuggerFlag) {
                case "interpreter":
                    MenterDebugger.logInterpreterEvaluation = arguments[1].isTrue();
                    break;
                case "lexer":
                    MenterDebugger.logLexedTokens = arguments[1].isTrue();
                    break;
                case "parser":
                    MenterDebugger.logParsedTokens = arguments[1].isTrue();
                    break;
                case "parser progress":
                    MenterDebugger.logParseProgress = arguments[1].isTrue();
                    break;
                case "interpreter resolve":
                    MenterDebugger.logInterpreterResolveSymbols = arguments[1].isTrue();
                    break;
                case "import order":
                    MenterDebugger.logInterpreterEvaluationOrder = arguments[1].isTrue();
                    break;

                default:
                    throw new IllegalArgumentException("Unknown debugger flag: " + debuggerFlag);
            }
        }

        return Value.empty();
    }

    public static Value stackTraceValues(Value[] arguments) {
        MenterDebugger.printSymbolValuesOnStackTrace.clear();
        for (Value argument : arguments) {
            MenterDebugger.printSymbolValuesOnStackTrace.add(argument.getValue().toString());
        }
        return Value.empty();
    }
}