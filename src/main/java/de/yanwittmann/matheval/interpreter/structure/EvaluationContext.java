package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.interpreter.MenterDebugger;
import de.yanwittmann.matheval.interpreter.core.CoreModuleCommon;
import de.yanwittmann.matheval.interpreter.core.CoreModuleDebug;
import de.yanwittmann.matheval.interpreter.core.CoreModuleIo;
import de.yanwittmann.matheval.interpreter.core.CoreModuleSystem;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.operator.Operator;
import de.yanwittmann.matheval.parser.Parser;
import de.yanwittmann.matheval.parser.ParserNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class EvaluationContext {

    private static final Logger LOG = LogManager.getLogger(EvaluationContext.class);

    private final EvaluationContext parentContext;
    private final Map<String, Value> variables;
    protected final static Map<String[], Function<Value[], Value>> nativeFunctions = new HashMap<>();

    static {
        registerNativeFunction(new String[]{"common.ter", "print"}, CoreModuleCommon::print);
        registerNativeFunction(new String[]{"common.ter", "range"}, CoreModuleCommon::range);

        registerNativeFunction(new String[]{"io.ter", "read"}, CoreModuleIo::apply);

        registerNativeFunction(new String[]{"system.ter", "getProperty"}, CoreModuleSystem::getProperty);
        registerNativeFunction(new String[]{"system.ter", "getEnv"}, CoreModuleSystem::getEnv);

        registerNativeFunction(new String[]{"debug.ter", "switch"}, CoreModuleDebug::debugSwitch);
        registerNativeFunction(new String[]{"debug.ter", "stackTraceValues"}, CoreModuleDebug::stackTraceValues);
    }

    public static void registerNativeFunction(String[] path, Function<Value[], Value> function) {
        nativeFunctions.put(path, function);
    }

    public EvaluationContext(EvaluationContext parentContext) {
        this(parentContext, new HashMap<>());
    }

    public EvaluationContext(EvaluationContext parentContext, Map<String, Value> variables) {
        this.parentContext = parentContext;
        this.variables = variables;
    }

    public EvaluationContext getParentContext() {
        return parentContext;
    }

    public Map<String, Value> getVariables() {
        return variables;
    }

    public void addVariable(String name, Value value) {
        variables.put(name, value);
    }

    public Value getVariable(String name) {
        final Value value = variables.get(name);
        if (value == null && parentContext != null) {
            return parentContext.getVariable(name);
        }
        return value;
    }

    public Value evaluate(Object nodeOrToken, GlobalContext globalContext, SymbolCreationMode symbolCreationMode, EvaluationContextLocalInformation localInformation) {
        Value result = null;

        final boolean isMultiExpressionNode = Parser.isType(nodeOrToken, ParserNode.NodeType.STATEMENT) || Parser.isType(nodeOrToken, ParserNode.NodeType.ROOT) || Parser.isType(nodeOrToken, ParserNode.NodeType.CODE_BLOCK);
        if (!isMultiExpressionNode) {
            localInformation.putStackFrame(globalContext, nodeOrToken);
        }

        if (nodeOrToken instanceof ParserNode) {
            final ParserNode node = (ParserNode) nodeOrToken;

            final boolean isDebuggerBreakpoint = MenterDebugger.haltOnEveryExecutionStep || (MenterDebugger.breakpointActivationCode != null && node.reconstructCode().equals(MenterDebugger.breakpointActivationCode.trim()));

            if (!isMultiExpressionNode) {
                if (MenterDebugger.logInterpreterEvaluationStyle > 0 || isDebuggerBreakpoint) {
                    if (MenterDebugger.logInterpreterEvaluationStyle == 2) {
                        System.out.println(createDebuggerPrintIndentation(localInformation) + node.reconstructCode());

                    } else if (MenterDebugger.logInterpreterEvaluationStyle == 1) {
                        System.out.print(node.reconstructCode());
                        if (!isDebuggerBreakpoint) System.out.println();
                        else System.out.print(" ");

                        if (isDebuggerBreakpoint) {
                            breakpointReached(localInformation, node);
                        }
                    }
                }
            }

            if (isMultiExpressionNode) {
                for (Object child : node.getChildren()) {
                    result = evaluate(child, globalContext, symbolCreationMode, localInformation);
                    if (child instanceof ParserNode && ((ParserNode) child).getType() == ParserNode.NodeType.RETURN_STATEMENT) {
                        break;
                    }
                }
            } else if (node.getType() == ParserNode.NodeType.RETURN_STATEMENT) {
                result = evaluate(node.getChildren().get(0), globalContext, symbolCreationMode, localInformation);

            } else if (node.getType() == ParserNode.NodeType.IMPORT_STATEMENT) {
                throw localInformation.createException("Import statements are not supported in the interpreter");

            } else if (node.getType() == ParserNode.NodeType.ARRAY) {
                final Map<Object, Value> array = new LinkedHashMap<>();
                final List<Object> children = node.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    array.put(new BigDecimal(i), evaluate(children.get(i), globalContext, symbolCreationMode, localInformation));
                }
                result = new Value(array);

            } else if (node.getType() == ParserNode.NodeType.MAP) {
                final Map<String, Value> map = new LinkedHashMap<>();
                for (Object child : node.getChildren()) {
                    final ParserNode childNode = (ParserNode) child;

                    final Object keyNode = childNode.getChildren().get(0);
                    final String key;
                    if (keyNode instanceof Value) {
                        key = ((Value) keyNode).toDisplayString();
                    } else if (keyNode instanceof Token) {
                        key = ((Token) keyNode).getValue();
                    } else if (keyNode instanceof ParserNode) {
                        key = evaluate(keyNode, globalContext, symbolCreationMode, localInformation).toDisplayString();
                    } else {
                        throw localInformation.createException("Invalid map key type: " + keyNode.getClass().getName());
                    }

                    final Value value = evaluate(childNode.getChildren().get(1), globalContext, symbolCreationMode, localInformation);
                    map.put(key, value);
                }
                result = new Value(map);

            } else if (node.getType() == ParserNode.NodeType.EXPRESSION) {
                final Object operator = node.getValue();
                if (operator instanceof Operator) {
                    final Operator op = (Operator) operator;
                    final int numberOfArguments = op.getNumberOfArguments();
                    int numberOfChildren = node.getChildren().size();

                    if (numberOfChildren != numberOfArguments) {
                        throw localInformation.createException("Operator " + op.getSymbol() + " requires " + numberOfArguments + " arguments, but " + numberOfChildren + " were given:");
                    }

                    final Value[] arguments = new Value[numberOfArguments];
                    for (int i = 0; i < numberOfArguments; i++) {
                        arguments[i] = evaluate(node.getChildren().get(i), globalContext, symbolCreationMode, localInformation);
                    }

                    result = op.evaluate(arguments);
                    if (result == null) {
                        throw localInformation.createException("Operator " + op.getSymbol() + " did not return a result; this is most likely due to an incomplete implementation of the operator.");
                    }
                }
            } else if (node.getType() == ParserNode.NodeType.IDENTIFIER_ACCESSED) {
                result = resolveSymbol(node, symbolCreationMode, globalContext, localInformation);

            } else if (node.getType() == ParserNode.NodeType.ASSIGNMENT) {
                final Value variable = evaluate(node.getChildren().get(0), globalContext, SymbolCreationMode.CREATE_IF_NOT_EXISTS, localInformation);
                final Value value = evaluate(node.getChildren().get(1), globalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, localInformation);

                variable.inheritValue(value);
                result = value;

                if (MenterDebugger.logInterpreterAssignments) {
                    LOG.info("Assigned value [{}] to variable [{}] from: {}", value, ParserNode.reconstructCode(node.getChildren().get(0)), node.reconstructCode());
                }

            } else if (node.getType() == ParserNode.NodeType.PARENTHESIS_PAIR) {
                if (node.getChildren().size() == 1) {
                    result = evaluate(node.getChildren().get(0), globalContext, symbolCreationMode, localInformation);
                } else {
                    result = new Value(node.getChildren().stream()
                            .map(child -> evaluate(child, globalContext, symbolCreationMode, localInformation))
                            .collect(Collectors.toList()));
                }

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_DECLARATION) {
                if (Parser.isKeyword(node.getChildren().get(0), "native")) {
                    final Value functionValue = evaluate(node.getChildren().get(1), globalContext, SymbolCreationMode.CREATE_IF_NOT_EXISTS, localInformation);
                    final String functionName = ((Token) node.getChildren().get(1)).getValue();
                    final List<Object> functionArguments = Parser.isType(node.getChildren().get(2), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(2)).getChildren() : null;
                    if (functionArguments == null) {
                        throw localInformation.createException("Function arguments are not a parenthesis pair");
                    }

                    if (!(this instanceof GlobalContext)) {
                        throw localInformation.createException("Native functions can only be declared in the global context");
                    }

                    final List<String> moduleNameCandidates = new ArrayList<>();
                    final GlobalContext thisGlobalContext = (GlobalContext) this;
                    thisGlobalContext.getModules().forEach(module -> moduleNameCandidates.add(module.getName()));
                    moduleNameCandidates.add(globalContext.getSourceName());

                    boolean foundNativeFunction = false;
                    for (String moduleNameCandidate : moduleNameCandidates) {
                        for (Map.Entry<String[], Function<Value[], Value>> nativeFunction : nativeFunctions.entrySet()) {
                            final String[] functionQualifier = nativeFunction.getKey();
                            final Function<Value[], Value> nativeFunctionValue = nativeFunction.getValue();

                            if (functionQualifier.length != 2) {
                                throw localInformation.createException("Invalid native function qualifier: " + Arrays.toString(functionQualifier) + ". Expected format: [module name, function name]");
                            }

                            if (functionQualifier[0].equals(moduleNameCandidate) && functionQualifier[1].equals(functionName)) {
                                functionValue.setValue(nativeFunctionValue);
                                result = functionValue;
                                foundNativeFunction = true;
                                break;
                            }
                        }
                        if (foundNativeFunction) {
                            break;
                        }
                    }

                    if (!foundNativeFunction) {
                        throw localInformation.createException("Native function [" + functionName + "] not found using candidates: " + moduleNameCandidates + "\nDefine custom functions using EvaluationContext.registerNativeFunction().");
                    }

                } else {
                    final Value functionValue = evaluate(node.getChildren().get(0), globalContext, SymbolCreationMode.CREATE_IF_NOT_EXISTS, localInformation);
                    final List<Object> functionArguments = Parser.isType(node.getChildren().get(1), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(1)).getChildren() : null;
                    if (functionArguments == null) {
                        throw localInformation.createException("Function arguments are not a parenthesis pair");
                    }
                    final ParserNode functionCode = (ParserNode) node.getChildren().get(2);

                    final MenterNodeFunction function = new MenterNodeFunction(globalContext, functionArguments, functionCode);
                    functionValue.setValue(function);

                    result = functionValue;
                }

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_INLINE) {
                // 0 is params, 1 is body
                final List<Object> functionArguments = Parser.isType(node.getChildren().get(0), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(0)).getChildren() : null;
                if (functionArguments == null) {
                    throw localInformation.createException("Function arguments are not a parenthesis pair");
                }

                final ParserNode functionCode = (ParserNode) node.getChildren().get(1);
                final MenterNodeFunction function = new MenterNodeFunction(globalContext, functionArguments, functionCode);
                result = new Value(function);

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_CALL) {
                final Value function = evaluate(node.getChildren().get(0), globalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, localInformation);
                if (!Parser.isType(node.getChildren().get(1), ParserNode.NodeType.PARENTHESIS_PAIR)) {
                    throw localInformation.createException("Function arguments are not a parenthesis pair");
                }

                final List<Value> functionParameters = makeFunctionArguments(node, globalContext, localInformation);
                result = evaluateFunction(function, functionParameters, globalContext, localInformation, ParserNode.reconstructCode(node.getChildren().get(0)));

            } else if (node.getType() == ParserNode.NodeType.CONDITIONAL) {
                final List<ParserNode> branches = node.getChildren().stream()
                        .filter(child -> Parser.isType(child, ParserNode.NodeType.CONDITIONAL_BRANCH))
                        .map(c -> (ParserNode) c)
                        .collect(Collectors.toList());

                boolean foundMatchingBranch = false;
                for (ParserNode branch : branches) {
                    if (branch.getChildren().size() == 1) { // else
                        result = evaluate(branch.getChildren().get(0), globalContext, symbolCreationMode, localInformation);
                        foundMatchingBranch = true;
                        break;

                    } else {
                        final Value condition = evaluate(branch.getChildren().get(0), globalContext, symbolCreationMode, localInformation);
                        if (condition.isTrue()) {
                            result = evaluate(branch.getChildren().get(1), globalContext, symbolCreationMode, localInformation);
                            foundMatchingBranch = true;
                            break;
                        }
                    }
                }

                if (!foundMatchingBranch) {
                    result = Value.empty();
                }
            } else if (node.getType() == ParserNode.NodeType.LOOP_FOR) { // for each loop
                final Object iteratorVariable = node.getChildren().get(0);
                final Value iteratorValue = evaluate(node.getChildren().get(1), globalContext, symbolCreationMode, localInformation);
                final Object loopCode = node.getChildren().get(2);

                final Value iteratorGetter = iteratorValue.access(new Value("iterator"));
                final Value iterator = evaluateFunction(iteratorGetter, Collections.singletonList(iteratorValue), globalContext, localInformation, "iterator");
                if (!iterator.getType().equals(PrimitiveValueType.ITERATOR.getType())) {
                    throw localInformation.createException("Iterator element did not provide iterable: " + iteratorValue);
                }
                final Iterator<Value> iteratorIterator = (Iterator<Value>) iterator.getValue();

                // might be a list of values or a single value
                final List<Value> variableValues = new ArrayList<>();

                if (iteratorVariable instanceof Token) {
                    variableValues.add(new Value(iteratorVariable));

                } else if (iteratorVariable instanceof ParserNode) {
                    final ParserNode varNode = (ParserNode) iteratorVariable;
                    if (Parser.isType(varNode, ParserNode.NodeType.PARENTHESIS_PAIR) || Parser.isType(varNode, ParserNode.NodeType.SQUARE_BRACKET_PAIR) ||
                        Parser.isType(varNode, ParserNode.NodeType.ARRAY)) {
                        for (Object child : varNode.getChildren()) {
                            variableValues.add(new Value(child));
                        }
                    } else {
                        throw localInformation.createException("Invalid iterator variable node: " + iteratorVariable);
                    }

                } else {
                    throw localInformation.createException("Iterator variable is not a token or a node");
                }

                final EvaluationContextLocalInformation loopLocalInformation = localInformation.deriveNewContext();
                variableValues.forEach(v -> loopLocalInformation.putLocalSymbol(v.getValue().toString(), v));


                while (iteratorIterator.hasNext()) {
                    final Value iteratorElement = iteratorIterator.next();
                    final Object iteratorElementValue = iteratorElement.getValue();

                    final int acceptedVariableCount = variableValues.size();
                    final int providedVariableCount = iteratorElement.size();

                    if (iteratorElementValue instanceof Map && providedVariableCount == 2) {
                        final Map<?, ?> element = (Map<?, ?>) iteratorElementValue;

                        if (acceptedVariableCount == 1) {
                            variableValues.get(0).setValue(element.get("value"));

                        } else if (acceptedVariableCount == 2) {
                            variableValues.get(0).setValue(element.get("key"));
                            variableValues.get(1).setValue(element.get("value"));

                        } else {
                            throw localInformation.createException("Invalid variable count [" + acceptedVariableCount + "] for iterator element: " + iteratorElement);
                        }
                    } else if (providedVariableCount == 1) {
                        if (acceptedVariableCount == 1) {
                            variableValues.get(0).setValue(iteratorElementValue);

                        } else {
                            throw localInformation.createException("Invalid variable count [" + acceptedVariableCount + "] for iterator element: " + iteratorElement);
                        }
                    } else {
                        throw localInformation.createException("Invalid iterator element: " + iteratorElement);
                    }

                    result = evaluate(loopCode, globalContext, symbolCreationMode, loopLocalInformation);
                }
            }

            if (result == null) {
                throw localInformation.createException("Node did not evaluate to anything: " + ParserNode.reconstructCode(node));
            }

            if (!isMultiExpressionNode) {
                if (MenterDebugger.logInterpreterEvaluationStyle > 1 || isDebuggerBreakpoint) {
                    if (MenterDebugger.logInterpreterEvaluationStyle == 2) {
                        System.out.println(createDebuggerPrintIndentation(localInformation) + "└─> " + result);
                    } else if (MenterDebugger.logInterpreterEvaluationStyle == 3) {
                        System.out.println(node.reconstructCode() + " --> " + result);

                        if (isDebuggerBreakpoint) {
                            breakpointReached(localInformation, node);
                        }
                    }
                }
            }

        } else if (nodeOrToken instanceof Token) {
            final Token node = (Token) nodeOrToken;

            if (node.getType() == Lexer.TokenType.IDENTIFIER) {
                result = resolveSymbol(node, symbolCreationMode, globalContext, localInformation);

            } else if (node.getType() == Lexer.TokenType.NUMBER_LITERAL) {
                result = new Value(new BigDecimal(node.getValue()));
            } else if (node.getType() == Lexer.TokenType.BOOLEAN_LITERAL) {
                result = new Value(Boolean.valueOf(node.getValue()));
            } else if (node.getType() == Lexer.TokenType.STRING_LITERAL) {
                result = new Value(node.getValue().substring(1, node.getValue().length() - 1));
            } else if (node.getType() == Lexer.TokenType.REGEX_LITERAL) {
                result = new Value(Pattern.compile(node.getValue()));
            } else if (Parser.isListable(node)) {
                result = new Value(node.getValue());
            }
        }

        if (!isMultiExpressionNode) {
            localInformation.popStackFrame();
        }

        return result;
    }

    private void breakpointReached(EvaluationContextLocalInformation localInformation, ParserNode node) {
        MenterDebugger.haltOnEveryExecutionStep = true;
        while (true) {
            final int action = MenterDebugger.waitForDebuggerResume();

            if (action == 0) {
                break;
            } else if (action == 1) {
                final StringBuilder sb = new StringBuilder();
                localInformation.appendStackTraceSymbols(sb, new MenterStackTraceElement(this.getParentContext() instanceof GlobalContext ? ((GlobalContext) this.getParentContext()) : null, node), true);
                System.out.println("Symbols:" + sb);
            } else if (action == 2) {
                localInformation.printStackTrace("Debugger stack trace:");
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            } else if (action == 3) {
                MenterDebugger.haltOnEveryExecutionStep = false;
                break;
            }

            System.out.print(node.reconstructCode() + " ");
        }
    }

    protected Value evaluateFunction(Value functionValue, List<Value> functionParameters, GlobalContext globalContext, EvaluationContextLocalInformation localInformation, String originalFunctionName) {
        try {
            localInformation.provideFunctionNameForNextStackTraceElement(originalFunctionName);

            if (!functionValue.isFunction()) {
                throw localInformation.createException("Value is not a function [" + functionValue + "]");
            }

            if (functionValue.getValue() instanceof MenterNodeFunction) {
                final MenterNodeFunction executableFunction = (MenterNodeFunction) functionValue.getValue();
                final List<String> functionArgumentNames = executableFunction.getArgumentNames();

                if (functionArgumentNames.size() != functionParameters.size()) {
                    throw localInformation.createException("Function [" + functionValue + "] requires " + functionArgumentNames.size() + " arguments, but " + functionParameters.size() + " were given");
                }

                final EvaluationContextLocalInformation functionLocalInformation = localInformation.deriveNewContext();
                for (int i = 0; i < functionArgumentNames.size(); i++) {
                    final String argumentName = functionArgumentNames.get(i);
                    final Value argumentValue = functionParameters.get(i);
                    functionLocalInformation.putLocalSymbol(argumentName, argumentValue);
                }

                functionLocalInformation.putLocalSymbol(executableFunction.getParentContext().getVariables());

                return evaluate(executableFunction.getBody(), globalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, functionLocalInformation);

            } else if (Objects.equals(functionValue.getType(), PrimitiveValueType.NATIVE_FUNCTION.getType())) { // native functions
                final Function<Value[], Value> nativeFunction = (Function<Value[], Value>) functionValue.getValue();
                final Value[] nativeFunctionArguments = functionParameters.toArray(new Value[0]);
                return nativeFunction.apply(nativeFunctionArguments);

            } else { // otherwise it must be a value function
                final MenterValueFunction executableFunction = (MenterValueFunction) functionValue.getValue();
                return executableFunction.apply(globalContext, functionValue.getSecondaryValue(), functionParameters, localInformation);
            }
        } catch (Exception e) {
            throw localInformation.createException(e.getMessage(), e);
        }
    }

    private List<Value> makeFunctionArguments(Object functionParameters, GlobalContext parameterGlobalContext, EvaluationContextLocalInformation localInformation) {
        if (!(functionParameters instanceof ParserNode)) {
            throw localInformation.createException("Function parameters are not a parenthesis pair\n" + functionParameters);
        }

        final ParserNode functionParametersNode = (ParserNode) functionParameters;

        final ParserNode parenthesisPair;
        if (Parser.isType(functionParametersNode, ParserNode.NodeType.PARENTHESIS_PAIR)) {
            parenthesisPair = functionParametersNode;
        } else {
            // check all child nodes
            parenthesisPair = functionParametersNode.getChildren().stream()
                    .filter(child -> Parser.isType(child, ParserNode.NodeType.PARENTHESIS_PAIR))
                    .map(child -> (ParserNode) child)
                    .reduce((first, second) -> second) // find last in stream
                    .orElse(null);
        }


        final List<Object> functionArguments = Parser.isType(parenthesisPair, ParserNode.NodeType.PARENTHESIS_PAIR) ? parenthesisPair.getChildren() : null;
        if (functionArguments == null) {
            throw localInformation.createException("Function arguments are not a parenthesis pair");
        }
        return functionArguments.stream()
                .map(argument -> evaluate(argument, parameterGlobalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, localInformation))
                .collect(Collectors.toList());
    }

    enum SymbolCreationMode {
        CREATE_IF_NOT_EXISTS,
        THROW_IF_NOT_EXISTS
    }

    private Value resolveSymbol(Object identifier, SymbolCreationMode symbolCreationMode, GlobalContext globalContext, EvaluationContextLocalInformation localInformation) {
        if (MenterDebugger.logInterpreterResolveSymbols) {
            LOG.info("Symbol resolve start: {}", ParserNode.reconstructCode(identifier));
        }

        final List<Object> identifiers = new ArrayList<>();
        if (identifier instanceof ParserNode) {
            final ParserNode node = (ParserNode) identifier;
            if (node.getType() == ParserNode.NodeType.IDENTIFIER_ACCESSED) {
                identifiers.addAll(node.getChildren());
            } else if (Parser.isLiteral(node)) {
                identifiers.add(evaluate(node, globalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, localInformation));
            }
        } else if (identifier instanceof Token) {
            final Token token = (Token) identifier;
            if (token.getType() == Lexer.TokenType.IDENTIFIER) {
                identifiers.add(token);
            }
        } else if (identifier instanceof Value || identifier instanceof String) {
            identifiers.add(identifier);
        }

        // handle code blocks/expressions
        for (int i = 0; i < identifiers.size(); i++) {
            if (identifiers.get(i) instanceof ParserNode) {
                final ParserNode node = (ParserNode) identifiers.get(i);
                if (node.getType() == ParserNode.NodeType.CODE_BLOCK || node.getType() == ParserNode.NodeType.EXPRESSION) {
                    final Value value = evaluate(node, globalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, localInformation);
                    identifiers.set(i, value);
                }
            } else if (identifiers.get(i) instanceof Token) {
                final Token token = (Token) identifiers.get(i);
                if (Parser.isLiteral(token)) {
                    final Value value = evaluate(token, globalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, localInformation);
                    identifiers.set(i, value);
                }
            }
        }

        if (identifiers.isEmpty()) {
            throw localInformation.createException("Cannot resolve symbol from " + ParserNode.reconstructCode(identifier));
        }
        if (MenterDebugger.logInterpreterResolveSymbols) {
            LOG.info("Symbol resolve: Split into identifiers: {}", identifiers);
        }

        final GlobalContext originalGlobalContext = globalContext;
        Value value = null;

        for (int i = 0; i < identifiers.size(); i++) {
            final Object id = identifiers.get(i);
            final Object nextId = i + 1 < identifiers.size() ? identifiers.get(i + 1) : null;
            final String stringKey = Module.ID_TO_KEY_MAPPER.apply(id);
            final String nextStringKey = Module.ID_TO_KEY_MAPPER.apply(nextId);

            final boolean isFinalIdentifier = id == identifiers.get(identifiers.size() - 1);
            final Value previousValue = value;

            if (value == null) {
                if (localInformation.hasLocalSymbol(stringKey)) {
                    value = localInformation.getLocalSymbol(stringKey);
                    if (MenterDebugger.logInterpreterResolveSymbols) {
                        LOG.info("Symbol resolve: [{}] from local symbols", stringKey);
                    }
                    continue;
                }

                final Value variable = globalContext.getVariable(stringKey);
                if (variable != null) {
                    value = variable;
                    if (MenterDebugger.logInterpreterResolveSymbols) {
                        LOG.info("Symbol resolve: [{}] from global variables", stringKey);
                    }
                    continue;
                }

                boolean foundImport = false;
                for (Import anImport : globalContext.getImports()) {
                    if (!anImport.isInline() && anImport.getAliasOrName().equals(stringKey)) {
                        final Module module = anImport.getModule();
                        if (module != null) {
                            globalContext = module.getParentContext();
                            localInformation = localInformation.deriveNewContext();
                            localInformation.setLocalSymbols(globalContext.getVariables());
                            // value = null; // is already null
                            foundImport = true;
                            if (MenterDebugger.logInterpreterResolveSymbols) {
                                LOG.info("Symbol resolve: [{}] from import: {}; switching to module context", stringKey, anImport);
                            }
                            break;
                        }
                    } else if (anImport.isInline() && anImport.getModule().containsSymbol(stringKey)) {
                        final Module module = anImport.getModule();
                        if (module != null) {
                            globalContext = module.getParentContext();
                            localInformation = localInformation.deriveNewContext();
                            localInformation.setLocalSymbols(globalContext.getVariables());
                            value = module.getParentContext().getVariable(stringKey);
                            foundImport = true;
                            if (MenterDebugger.logInterpreterResolveSymbols) {
                                LOG.info("Symbol resolve: [{}] from inline import: {}; switching to module context", stringKey, anImport);
                            }
                            break;
                        }
                    }
                }
                if (foundImport) continue;

                if (id instanceof Value) {
                    value = (Value) id;
                    if (MenterDebugger.logInterpreterResolveSymbols) {
                        LOG.info("Symbol resolve: [{}] from value", stringKey);
                    }
                    continue;
                }

                if (id instanceof ParserNode) {
                    final ParserNode node = (ParserNode) id;
                    if (node.getType() == ParserNode.NodeType.MAP || node.getType() == ParserNode.NodeType.CODE_BLOCK ||
                        node.getType() == ParserNode.NodeType.ARRAY) {
                        value = evaluate(node, globalContext, symbolCreationMode, localInformation);
                        if (MenterDebugger.logInterpreterResolveSymbols) {
                            LOG.info("Symbol resolve: [{}] from evaluated node", value);
                        }
                        continue;
                    }
                }

                if ("symbols".equals(stringKey) && globalContext != originalGlobalContext) {
                    value = new Value(globalContext.getVariables());
                    if (MenterDebugger.logInterpreterResolveSymbols) {
                        LOG.info("Symbol resolve: [{}] from symbols", value);
                    }
                    continue;
                }

            } else {
                if (Parser.isType(id, ParserNode.NodeType.FUNCTION_CALL)) {
                    final List<Value> functionParameters = makeFunctionArguments(id, originalGlobalContext, localInformation);
                    try {
                        value = evaluateFunction(value, functionParameters, globalContext, localInformation, ParserNode.reconstructCode(identifiers.get(i - 1)));
                    } catch (Exception e) {
                        throw localInformation.createException(e.getMessage() + ": " + identifiers.get(i - 1), e);
                    }
                    continue;

                } else {
                    final Value accessAs = id instanceof Value ? (Value) id : new Value(getTokenOrNodeValue(id));
                    value = value.access(accessAs);

                    if (value != null) {
                        if (MenterDebugger.logInterpreterResolveSymbols) {
                            LOG.info("Symbol resolve: [{}] from accessing previous value: {}", stringKey, previousValue);
                        }
                        continue;

                    } else if (SymbolCreationMode.CREATE_IF_NOT_EXISTS.equals(symbolCreationMode)) {
                        value = Value.empty();
                        if (!previousValue.create(accessAs, value, isFinalIdentifier)) {
                            value = null;
                        } else if (MenterDebugger.logInterpreterResolveSymbols) {
                            LOG.info("Symbol resolve: [{}] from creating new value on previous value: {}", stringKey, previousValue);
                        }
                        continue;
                    }
                }
            }

            if (SymbolCreationMode.THROW_IF_NOT_EXISTS == symbolCreationMode) {
                final List<String> candidates = findMostLikelyCandidates(globalContext, previousValue, stringKey);

                throw localInformation.createException("Cannot resolve symbol '" + stringKey + "' on " + ParserNode.reconstructCode(identifier) +
                                                       (candidates.isEmpty() ? "" : "; did you mean " + candidates.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ")) + "?"));

            } else if (SymbolCreationMode.CREATE_IF_NOT_EXISTS == symbolCreationMode) {
                if (isFinalIdentifier) {
                    value = Value.empty();
                } else {
                    value = new Value(new LinkedHashMap<>());
                }

                localInformation.putLocalSymbol(stringKey, value);
                if (MenterDebugger.logInterpreterResolveSymbols) {
                    LOG.info("Symbol resolve: [{}] from creating new value", stringKey);
                }
            }
        }

        return value;
    }

    private List<String> findMostLikelyCandidates(GlobalContext globalContext, Value previousValue, String identifierAccessed) {
        final List<Object> candidates = new ArrayList<>();

        if (previousValue != null) {
            if (previousValue.getType().equals(PrimitiveValueType.OBJECT.getType())) {
                candidates.addAll(previousValue.getMap().keySet());
            }
            candidates.addAll(previousValue.getValueFunctionCandidates().stream()
                    .map(Map.Entry::getValue)
                    .map(Map::keySet)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));
        } else {
            candidates.addAll(globalContext.getVariables().keySet());
            for (Import anImport : globalContext.getImports()) {
                if (anImport.getModule() != null) {
                    if (anImport.isInline()) {
                        candidates.addAll(anImport.getModule().getSymbols().stream()
                                .map(t -> {
                                    if (t instanceof Token) {
                                        return ((Token) t).getValue();
                                    } else if (t instanceof ParserNode) {
                                        return ParserNode.reconstructCode(t);
                                    } else {
                                        return t.toString();
                                    }
                                }).collect(Collectors.toList()));
                    } else {
                        candidates.add(anImport.getAliasOrName());
                    }
                }
            }
        }

        final List<String> stringCandidates = candidates.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        final BiFunction<String, String, Integer> scoreFunction = (s1, s2) -> {
            if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) {
                return 0;
            }
            s1 = s1.toLowerCase();
            s2 = s2.toLowerCase();

            int score = 0;
            int currentMatchIndex = 0;
            boolean isMatchingSequence = true;
            for (int i = 0; i < s1.length() && currentMatchIndex < s2.length(); i++) {
                if (s1.charAt(i) == s2.charAt(currentMatchIndex)) {
                    if (isMatchingSequence) {
                        score += 1;
                    } else {
                        score += 0.5;
                    }
                    isMatchingSequence = true;
                    currentMatchIndex++;
                } else {
                    isMatchingSequence = false;
                }
            }

            return score;
        };

        return stringCandidates.stream()
                .sorted(Comparator.comparingInt(s -> -scoreFunction.apply(s, identifierAccessed)))
                .limit(MenterDebugger.stackTraceUnknownSymbolSuggestions)
                .collect(Collectors.toList());
    }

    public Object getTokenOrNodeValue(Object node) {
        if (node instanceof Token) {
            return ((Token) node).getValue();
        } else if (node instanceof ParserNode) {
            return ((ParserNode) node).getValue();
        }
        return null;
    }

    private String createDebuggerPrintIndentation(EvaluationContextLocalInformation localInformation) {
        return IntStream.range(1, localInformation.getStackTrace().size()).mapToObj(x -> "| ").collect(Collectors.joining());
    }
}
