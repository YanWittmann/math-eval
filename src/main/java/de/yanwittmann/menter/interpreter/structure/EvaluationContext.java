package de.yanwittmann.menter.interpreter.structure;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.structure.value.CustomType;
import de.yanwittmann.menter.interpreter.structure.value.NativeFunction;
import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.lexer.Lexer.TokenType;
import de.yanwittmann.menter.lexer.Token;
import de.yanwittmann.menter.operator.Operator;
import de.yanwittmann.menter.parser.Parser;
import de.yanwittmann.menter.parser.ParserNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class EvaluationContext {

    private static final Logger LOG = LogManager.getLogger(EvaluationContext.class);

    private final Map<String, Value> variables;
    protected final static Map<String[], NativeFunction> nativeFunctions = new HashMap<>();

    static {
        try {
            Class.forName("de.yanwittmann.menter.interpreter.core.CoreModuleIo");
            Class.forName("de.yanwittmann.menter.interpreter.core.CoreModuleSystem");
            Class.forName("de.yanwittmann.menter.interpreter.core.CoreModuleDebug");
            Class.forName("de.yanwittmann.menter.interpreter.core.CoreModuleMath");
            Class.forName("de.yanwittmann.menter.interpreter.core.CoreModuleReflection");
            Class.forName("de.yanwittmann.menter.interpreter.core.CoreModuleCmdPlot");
        } catch (ClassNotFoundException e) {
            LOG.error("Failed to load core module", e);
        }
    }

    public EvaluationContext() {
        this(new HashMap<>());
    }

    public EvaluationContext(Map<String, Value> variables) {
        this.variables = variables;
    }

    public static void registerNativeFunction(String context, String module, NativeFunction function) {
        nativeFunctions.put(new String[]{context, module}, function);
    }

    public static void registerNativeFunction(String context, String module, Function<List<Value>, Value> function) {
        nativeFunctions.put(new String[]{context, module}, (evaluationContext, localInformation, values) -> function.apply(values));
    }

    public Map<String, Value> getVariables() {
        return variables;
    }

    public void addVariable(String name, Value value) {
        variables.put(name, value);
    }

    public void removeVariable(String name) {
        variables.remove(name);
    }

    public Value getVariable(String name) {
        return variables.get(name);
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

            if (isDebuggerBreakpoint) {
                breakpointReached(localInformation, node);
            }

            if (!isMultiExpressionNode) {
                if (MenterDebugger.logInterpreterEvaluationStyle > 0 || isDebuggerBreakpoint) {
                    if (MenterDebugger.logInterpreterEvaluationStyle == 2) {
                        MenterDebugger.printer.println(createDebuggerPrintIndentation(localInformation) + node.reconstructCode());

                    } else if (MenterDebugger.logInterpreterEvaluationStyle == 1) {
                        MenterDebugger.printer.print(node.reconstructCode());
                        if (!isDebuggerBreakpoint) MenterDebugger.printer.println();
                        else MenterDebugger.printer.print(" ");

                        if (isDebuggerBreakpoint) {
                            breakpointReached(localInformation, node);
                        }
                    }
                }
            }

            if (isMultiExpressionNode) {
                for (Object child : node.getChildren()) {
                    result = evaluate(child, globalContext, symbolCreationMode, localInformation);
                    if (result.isReturn() || result.isBreak() || result.isContinue()) {
                        break;
                    }
                }
            } else if (node.getType() == ParserNode.NodeType.RETURN_STATEMENT) {
                result = evaluate(node.getChildren().get(0), globalContext, symbolCreationMode, localInformation);
                result.setReturn(true);

            } else if (node.getType() == ParserNode.NodeType.IMPORT_STATEMENT) {
                throw localInformation.createException("Import statements are not supported in the interpreter");

            } else if (node.getType() == ParserNode.NodeType.ARRAY) {
                result = new Value(new LinkedHashMap<>());

                final List<Object> children = node.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    final EvaluationContextLocalInformation mapElementResolveLocalInformation = localInformation.deriveNewContext();
                    mapElementResolveLocalInformation.putSelf(result);

                    final Value evaluated = evaluate(children.get(i), globalContext, symbolCreationMode, mapElementResolveLocalInformation);
                    result.create(new Value(i), evaluated, true);
                }

            } else if (node.getType() == ParserNode.NodeType.MAP) {
                result = new Value(new LinkedHashMap<>());

                final Map<Object, Object> keyValues = new LinkedHashMap<>();

                for (Object child : node.getChildren()) {
                    final EvaluationContextLocalInformation mapElementResolveLocalInformation = localInformation.deriveNewContext();
                    mapElementResolveLocalInformation.putSelf(result);

                    final ParserNode childNode = (ParserNode) child;

                    final Object keyNode = childNode.getChildren().get(0);
                    final Object key;
                    if (keyNode instanceof Value) {
                        key = ((Value) keyNode).getValue();
                    } else if (keyNode instanceof Token) {
                        if (Parser.isLiteral(keyNode)) {
                            key = evaluate(keyNode, globalContext, symbolCreationMode, mapElementResolveLocalInformation).getValue();
                        } else {
                            key = ((Token) keyNode).getValue();
                        }
                    } else if (keyNode instanceof ParserNode) {
                        key = evaluate(keyNode, globalContext, symbolCreationMode, mapElementResolveLocalInformation).toDisplayString();
                    } else {
                        throw mapElementResolveLocalInformation.createException("Invalid map key type: " + keyNode.getClass().getName());
                    }

                    keyValues.put(key, childNode.getChildren().get(1));
                }

                // before
                for (Map.Entry<Object, Object> keyValue : keyValues.entrySet()) {
                    final Object key = keyValue.getKey();
                    final Object value = keyValue.getValue();

                    if ("$extends".equals(key)) {
                        final EvaluationContextLocalInformation mapElementResolveLocalInformation = localInformation.deriveNewContext();
                        mapElementResolveLocalInformation.putSelf(result);

                        if (!(value instanceof ParserNode)) {
                            throw localInformation.createException("Invalid $extends value, expected a constructor call: " + value.getClass().getName());
                        }

                        final ParserNode topLevelNode = (ParserNode) value;
                        final List<ParserNode> multiExtendsNodes = new ArrayList<>();

                        if (ParserNode.NodeType.ARRAY == topLevelNode.getType()) {
                            // get all child nodes and add them to the multiExtendsNodes list
                            for (Object child : topLevelNode.getChildren()) {
                                if (child instanceof ParserNode) {
                                    multiExtendsNodes.add((ParserNode) child);
                                } else {
                                    throw localInformation.createException("Invalid $extends value, expected an array of constructor calls: " + child.getClass().getName());
                                }
                            }

                        } else if (ParserNode.NodeType.FUNCTION_CALL == topLevelNode.getType()) {
                            multiExtendsNodes.add(topLevelNode);

                        } else {
                            throw localInformation.createException("Invalid $extends value, expected constructor call or an array of constructor calls: " + topLevelNode.getType());
                        }

                        for (ParserNode extendsNode : multiExtendsNodes) {
                            final ParserNode constructorCallNode = new ParserNode(ParserNode.NodeType.CONSTRUCTOR_CALL, null, extendsNode.getChildren());
                            final Value extendsObject = evaluate(constructorCallNode, globalContext, symbolCreationMode, mapElementResolveLocalInformation);
                            if (!PrimitiveValueType.isType(extendsObject, PrimitiveValueType.OBJECT)) {
                                throw localInformation.createException("Invalid $extends value, expected an object: " + extendsObject.getType());
                            }

                            for (Map.Entry<Object, Value> extendsEntry : extendsObject.getMap().entrySet()) {
                                result.create(new Value(extendsEntry.getKey()), extendsEntry.getValue(), true);
                            }
                        }

                    } else if ("$fields".equals(key)) {
                        final EvaluationContextLocalInformation mapElementResolveLocalInformation = localInformation.deriveNewContext();
                        mapElementResolveLocalInformation.putSelf(result);

                        if (!(value instanceof ParserNode)) {
                            throw localInformation.createException("Invalid $fields value, expected a list of attributes: " + value.getClass().getName());
                        }

                        final ParserNode listOfFieldsNode = (ParserNode) value;
                        if (!(listOfFieldsNode.getType().equals(ParserNode.NodeType.ARRAY))) {
                            throw localInformation.createException("Invalid $fields value, expected a list of attributes: " + listOfFieldsNode.getType());
                        }

                        final List<String> fieldsNames = new ArrayList<>();
                        for (Object child : listOfFieldsNode.getChildren()) {
                            if (!(child instanceof Token)) {
                                throw localInformation.createException("Invalid $fields value, token is not an identifier: " + child);
                            }

                            final Token fieldToken = (Token) child;
                            if (!fieldToken.getType().equals(TokenType.IDENTIFIER)) {
                                throw localInformation.createException("Invalid fields value, token is not an identifier: " + fieldToken.getType());
                            }

                            fieldsNames.add(fieldToken.getValue());
                        }

                        final Value args = localInformation.getLocalSymbol("args");
                        for (String fieldName : fieldsNames) {
                            final Value argsValue = args.access(new Value(fieldName));
                            if (argsValue == null) {
                                throw localInformation.createException("Invalid $fields value, field not found in args: " + fieldName);
                            }

                            result.create(new Value(fieldName), argsValue, true);
                        }
                    }
                }

                // middle
                for (Map.Entry<Object, Object> keyValue : keyValues.entrySet()) {
                    final Object key = keyValue.getKey();
                    final Object value = keyValue.getValue();

                    final boolean isConstructorValue;
                    if (key instanceof String) {
                        isConstructorValue = ((String) key).startsWith("$");
                    } else {
                        isConstructorValue = false;
                    }

                    if (isConstructorValue) {
                        if ("$init".equals(key) || "$extends".equals(key) || "$fields".equals(key)) {
                            continue;
                        }

                        // currently no different to without $, but maybe in the future (with private fields?)
                        final String keyString = (String) key;
                        final String keyStringWithoutPrefix = keyString.substring(1);

                        final EvaluationContextLocalInformation mapElementResolveLocalInformation = localInformation.deriveNewContext();
                        mapElementResolveLocalInformation.putSelf(result);

                        final Value evaluated = evaluate(value, globalContext, symbolCreationMode, mapElementResolveLocalInformation);
                        result.create(new Value(keyStringWithoutPrefix), evaluated, true);

                    } else {
                        final EvaluationContextLocalInformation mapElementResolveLocalInformation = localInformation.deriveNewContext();
                        mapElementResolveLocalInformation.putSelf(result);

                        final Value evaluated = evaluate(value, globalContext, symbolCreationMode, mapElementResolveLocalInformation);
                        result.create(new Value(key), evaluated, true);
                    }
                }

                // after
                for (Map.Entry<Object, Object> keyValue : keyValues.entrySet()) {
                    final Object key = keyValue.getKey();
                    final Object value = keyValue.getValue();

                    if ("$init".equals(key)) {
                        // $init is a constructor function: must be called last and takes no arguments

                        final EvaluationContextLocalInformation mapElementResolveLocalInformation = localInformation.deriveNewContext();
                        mapElementResolveLocalInformation.putSelf(result);

                        final Value initFunction = evaluate(value, globalContext, symbolCreationMode, mapElementResolveLocalInformation);
                        evaluateFunction("$init", initFunction, globalContext, mapElementResolveLocalInformation);

                    }
                }


            } else if (node.getType() == ParserNode.NodeType.EXPRESSION) {
                final Object operator = node.getValue();
                if (operator instanceof Operator) {
                    final Operator op = (Operator) operator;
                    final int numberOfArguments = op.getArgumentCount();
                    int numberOfChildren = node.getChildren().size();

                    if (numberOfChildren != numberOfArguments) {
                        throw localInformation.createException("Operator " + op.getSymbol() + " requires " + numberOfArguments + " arguments, but " + numberOfChildren + " were given:");
                    }

                    final Value[] arguments = new Value[numberOfArguments];
                    for (int i = 0; i < numberOfArguments; i++) {
                        arguments[i] = evaluate(node.getChildren().get(i), globalContext, symbolCreationMode, localInformation);
                    }

                    try {
                        result = op.evaluate(arguments);
                    } catch (Exception e) {
                        throw localInformation.createException(e);
                    }
                    if (result == null) {
                        throw localInformation.createException("Operator " + op.getSymbol() + " did not return a result; this is most likely due to an incomplete implementation of the operator.");
                    }
                }
            } else if (node.getType() == ParserNode.NodeType.IDENTIFIER_ACCESSED) {
                result = resolveSymbol(node, symbolCreationMode, globalContext, localInformation);

            } else if (node.getType() == ParserNode.NodeType.ASSIGNMENT) {
                if (!isAssignmentTargetFunctionCall(node.getChildren().get(0))) {
                    final String hint;
                    if (Parser.isType(node.getChildren().get(0), ParserNode.NodeType.IDENTIFIER_ACCESSED)) {
                        final ParserNode identifierNode = (ParserNode) node.getChildren().get(0);
                        final ParserNode identifierNodeClone = new ParserNode(identifierNode.getType(), identifierNode.getValue(), identifierNode.getChildren().subList(0, identifierNode.getChildren().size() - 1));
                        final ParserNode functionCallNode = (ParserNode) identifierNode.getChildren().get(identifierNode.getChildren().size() - 1);
                        final ParserNode parenthesisPairNode = (ParserNode) functionCallNode.getChildren().get(0);

                        hint = "To define a function on an object, use the '->' arrow syntax: " + identifierNodeClone.reconstructCode() + " = " + parenthesisPairNode.reconstructCode() + " -> { ... }";
                    } else {
                        hint = "Assignments are not allowed on function calls. Try removing the parentheses.";
                    }

                    throw localInformation.createException("Cannot assign to " + ParserNode.reconstructCode(node.getChildren().get(0)) + "\n" + hint);
                }

                final Value value = evaluate(node.getChildren().get(1), globalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, localInformation);
                final Value variable = evaluate(node.getChildren().get(0), globalContext, SymbolCreationMode.CREATE_IF_NOT_EXISTS, localInformation);

                if (value.isFunction() && !value.hasTaggedAdditionalInformation(Value.TAG_KEY_FUNCTION_CLOSURE_CONTEXT)) {
                    value.setTagParentFunctionClosureContext(globalContext);
                    value.setTagParentFunctionClosureLocalInformation(localInformation);
                }

                if (!(node.getValue() instanceof Operator)) {
                    throw localInformation.createException("Invalid assignment operator: " + node.getValue());
                }
                final Operator operator = (Operator) node.getValue();
                if (!(operator.getSymbol().equals("=") || variable.isEmpty())) {
                    if (operator.getArgumentCount() != 2) {
                        throw localInformation.createException("Invalid assignment operator, must take two arguments: " + operator.getSymbol());
                    }
                    final Value operationResult = operator.evaluate(variable, value);
                    variable.inheritValue(operationResult);

                } else {
                    variable.inheritValue(value);
                }
                result = variable;

                if (MenterDebugger.logInterpreterAssignments) {
                    MenterDebugger.printer.format("Assignment: [%s] = [%s] from: %s%n", ParserNode.reconstructCode(node.getChildren().get(0)), variable, node.reconstructCode());
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
                        for (Map.Entry<String[], NativeFunction> nativeFunction : nativeFunctions.entrySet()) {
                            final String[] functionQualifier = nativeFunction.getKey();
                            final NativeFunction nativeFunctionValue = nativeFunction.getValue();

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
                    if (!(node.getChildren().get(2) instanceof ParserNode)) {
                        node.getChildren().set(2, new ParserNode(ParserNode.NodeType.CODE_BLOCK, null, Collections.singletonList(node.getChildren().get(2))));
                    }
                    final ParserNode functionCode = (ParserNode) node.getChildren().get(2);

                    final MenterNodeFunction function = new MenterNodeFunction(globalContext, functionArguments, functionCode);
                    functionValue.setValue(function);
                    functionValue.setTagParentFunctionClosureLocalInformation(localInformation);
                    functionValue.setTagParentFunctionClosureContext(globalContext);

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
                result.setTagParentFunctionClosureLocalInformation(localInformation);
                result.setTagParentFunctionClosureContext(globalContext);

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_CALL) {
                final Value function = evaluate(node.getChildren().get(0), globalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, localInformation);
                if (!Parser.isType(node.getChildren().get(1), ParserNode.NodeType.PARENTHESIS_PAIR)) {
                    throw localInformation.createException("Function arguments are not a parenthesis pair");
                }

                final List<Value> functionParameters = makeFunctionArguments(node, globalContext, localInformation);
                result = evaluateFunction(function, functionParameters, globalContext, localInformation, ParserNode.reconstructCode(node.getChildren().get(0)));

            } else if (node.getType() == ParserNode.NodeType.OPERATOR_FUNCTION) {
                final Operator operator = (Operator) node.getValue();
                final NativeFunction function = (context, li, parameters) -> operator.evaluate(parameters);
                return new Value(function);

            } else if (node.getType() == ParserNode.NodeType.CONDITIONAL) {
                final List<ParserNode> branches = node.getChildren().stream()
                        .filter(child -> Parser.isType(child, ParserNode.NodeType.CONDITIONAL_BRANCH))
                        .map(c -> (ParserNode) c)
                        .collect(Collectors.toList());

                final EvaluationContextLocalInformation conditionalContext = localInformation.deriveNewContext();

                boolean foundMatchingBranch = false;
                for (ParserNode branch : branches) {
                    if (branch.getChildren().size() == 1) { // else
                        result = evaluate(branch.getChildren().get(0), globalContext, symbolCreationMode, conditionalContext);
                        foundMatchingBranch = true;
                        break;

                    } else {
                        final Value condition = evaluate(branch.getChildren().get(0), globalContext, symbolCreationMode, conditionalContext);
                        if (condition.isTrue()) {
                            result = evaluate(branch.getChildren().get(1), globalContext, symbolCreationMode, conditionalContext);
                            foundMatchingBranch = true;
                            break;
                        }
                    }
                }

                if (!foundMatchingBranch) {
                    result = Value.empty();
                }

            } else if (node.getType() == ParserNode.NodeType.LOOP_FOR) {
                result = forLoop(node, globalContext, symbolCreationMode, localInformation);

            } else if (node.getType() == ParserNode.NodeType.LOOP_WHILE) {
                result = whileLoop(node, globalContext, symbolCreationMode, localInformation);

            } else if (node.getType() == ParserNode.NodeType.CONSTRUCTOR_CALL) {
                final Value constructorIdentifier = evaluate(node.getChildren().get(0), globalContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, localInformation);
                final List<Value> constructorParameters = makeFunctionArguments(node, globalContext, localInformation);

                if (constructorIdentifier.getValue() instanceof Class) {
                    result = new Value(CustomType.createInstance((Class<? extends CustomType>) constructorIdentifier.getValue(), constructorParameters));

                } else if (PrimitiveValueType.isType(constructorIdentifier, PrimitiveValueType.FUNCTION)) {
                    result = evaluateFunction(constructorIdentifier, constructorParameters, globalContext, localInformation, ParserNode.reconstructCode(node.getChildren().get(0)),
                            (functionContext, args) -> {
                                // create "args" Value of type Map with the arguments
                                final Value argsValue = new Value(new LinkedHashMap<>());
                                for (Map.Entry<String, Value> nameValue : args.entrySet()) {
                                    argsValue.create(new Value(nameValue.getKey()), nameValue.getValue(), true);
                                }
                                functionContext.putLocalSymbolOnTop("args", argsValue);
                            });

                } else {
                    throw localInformation.createException("Constructor call is not a function or a class: " + constructorIdentifier.getValue());
                }
            }

            if (result == null) {
                throw localInformation.createException("Node did not evaluate to anything: " + ParserNode.reconstructCode(node));
            }

            if (!isMultiExpressionNode) {
                if (MenterDebugger.logInterpreterEvaluationStyle > 1 || isDebuggerBreakpoint) {
                    if (MenterDebugger.logInterpreterEvaluationStyle == 2) {
                        MenterDebugger.printer.println(createDebuggerPrintIndentation(localInformation) + CORNER + HORIZONTAL_LINE + ARROW_HEAD + SPACE + result);
                    } else if (MenterDebugger.logInterpreterEvaluationStyle == 3) {
                        MenterDebugger.printer.println(node.reconstructCode() + SPACE + HORIZONTAL_LINE + HORIZONTAL_LINE + ARROW_HEAD + SPACE + result);

                        if (isDebuggerBreakpoint) {
                            breakpointReached(localInformation, node);
                        }
                    }
                }
            }

        } else if (nodeOrToken instanceof Token) {
            final Token node = (Token) nodeOrToken;

            if (node.getType() == TokenType.IDENTIFIER) {
                result = resolveSymbol(node, symbolCreationMode, globalContext, localInformation);

            } else if (node.getType() == TokenType.NUMBER_LITERAL) {
                result = new Value(new BigDecimal(node.getValue()));
            } else if (node.getType() == TokenType.BOOLEAN_LITERAL) {
                result = new Value(Boolean.valueOf(node.getValue()));
            } else if (node.getType() == TokenType.STRING_LITERAL) {
                result = new Value(node.getValue().substring(1, node.getValue().length() - 1));
            } else if (node.getType() == TokenType.REGEX_LITERAL) {
                final String regexString = node.getValue(); // e.g. r/.+/i split into ".+", "i"
                final String patternAndFlags = regexString.substring(2);
                final int lastSlashIndex = patternAndFlags.lastIndexOf('/');
                final String pattern = patternAndFlags.substring(0, lastSlashIndex);
                final String flags = patternAndFlags.substring(lastSlashIndex + 1);
                final int flagsInt = parseRegexFlags(flags);

                result = new Value(Pattern.compile(pattern, flagsInt));

            } else if (node.getType() == TokenType.OTHER_LITERAL) {
                final String value = node.getValue();
                if (value.equals("null")) {
                    return Value.empty();
                } else {
                    return new Value(value);
                }

            } else if (Parser.isKeyword(node, "null") || Parser.isType(node, TokenType.PASS) ||
                       Parser.isType(node, TokenType.BREAK) || Parser.isType(node, TokenType.CONTINUE)) {

                result = Value.empty();
                if (Parser.isType(node, TokenType.BREAK)) {
                    result.setBreak(true);
                } else if (Parser.isType(node, TokenType.CONTINUE)) {
                    result.setContinue(true);
                }

            } else if (Parser.isListable(node)) {
                result = new Value(node.getValue());
            }
        }

        if (!isMultiExpressionNode) {
            localInformation.popStackFrame();
        }

        return result;
    }

    private int parseRegexFlags(String flags) {
        int result = 0;
        for (char flag : flags.toCharArray()) {
            switch (flag) {
                case 'i':
                    result |= Pattern.CASE_INSENSITIVE;
                    break;
                case 'm':
                    result |= Pattern.MULTILINE;
                    break;
                case 's':
                    result |= Pattern.DOTALL;
                    break;
                case 'u':
                    result |= Pattern.UNICODE_CASE;
                    break;
                case 'x':
                    result |= Pattern.COMMENTS;
                    break;
                default:
                    throw new RuntimeException("Unknown regex flag: " + flag);
            }
        }
        return result;
    }

    public Value forLoop(ParserNode originNode, GlobalContext globalContext, SymbolCreationMode symbolCreationMode, EvaluationContextLocalInformation localInformation) {
        final Object iteratorVariable = originNode.getChildren().get(0);
        final Value iteratorValue = evaluate(originNode.getChildren().get(1), globalContext, symbolCreationMode, localInformation);
        final Object loopCode = originNode.getChildren().get(2);

        final Value iteratorGetter = iteratorValue.access(new Value("iterator"));
        final Value iterator = evaluateFunction(iteratorGetter, Collections.singletonList(iteratorValue), globalContext, localInformation, "iterator");

        if (!iterator.getType().equals(PrimitiveValueType.ITERATOR.getType())) {
            throw localInformation.createException("Iterator element did not provide iterable: " + iteratorValue);
        }
        @SuppressWarnings("unchecked") final Iterator<Value> iteratorIterator = (Iterator<Value>) iterator.getValue();


        // might be a list of values or a single value
        final List<String> variableNames = new ArrayList<>();

        if (iteratorVariable instanceof Token) {
            variableNames.add(((Token) iteratorVariable).getValue());

        } else if (iteratorVariable instanceof ParserNode) {
            final ParserNode varNode = (ParserNode) iteratorVariable;
            if (Parser.isType(varNode, ParserNode.NodeType.PARENTHESIS_PAIR) || Parser.isType(varNode, ParserNode.NodeType.SQUARE_BRACKET_PAIR) ||
                Parser.isType(varNode, ParserNode.NodeType.ARRAY)) {
                for (Object child : varNode.getChildren()) {
                    variableNames.add(((Token) child).getValue());
                }
            } else {
                throw localInformation.createException("Invalid iterator variable node: " + iteratorVariable);
            }

        } else {
            throw localInformation.createException("Iterator variable is not a token or a node");
        }


        final int requestedParameterCount = variableNames.size();
        Value result = Value.empty();

        while (iteratorIterator.hasNext()) {
            final EvaluationContextLocalInformation loopLocalInformation = localInformation.deriveNewContext();

            final Value iteratorElement = iteratorIterator.next();

            if (iteratorElement.isMapAnArray()) {
                final LinkedHashMap<Object, Value> inputParameterValues = iteratorElement.getMap();
                final int actualParameterCount = inputParameterValues.size();

                if (actualParameterCount == requestedParameterCount) {
                    int i = 0;
                    for (Map.Entry<Object, Value> entry : inputParameterValues.entrySet()) {
                        final String variableName = variableNames.get(i);
                        final Value variableValue = entry.getValue();

                        loopLocalInformation.putLocalSymbol(variableName, variableValue);
                        i++;
                    }

                } else if (actualParameterCount == 2 && requestedParameterCount == 1) {
                    final String variableName = variableNames.get(0);
                    // (key, value) --> (value), ignore key
                    final Iterator<Value> parameterElementIterator = inputParameterValues.values().iterator();
                    parameterElementIterator.next();
                    final Value variableValue = parameterElementIterator.next();

                    localInformation.putLocalSymbol(variableName, variableValue);

                } else {
                    throw localInformation.createException("Expected " + requestedParameterCount + " variables, but got " + actualParameterCount + " for iterator element: " + iteratorElement);
                }
            } else {
                if (requestedParameterCount == 1) {
                    final String variableName = variableNames.get(0);
                    loopLocalInformation.putLocalSymbol(variableName, iteratorElement);

                } else {
                    throw localInformation.createException("Iterator element is " + iteratorElement.getType() + " but expected object: " + iteratorElement);
                }
            }

            result = evaluate(loopCode, globalContext, symbolCreationMode, loopLocalInformation);

            if (result.unwrapBreak()) {
                break;
            }
            result.unwrapContinue();
        }

        return result;
    }

    public Value forLoop(Value self, Value evaluatorFunction, GlobalContext globalContext, EvaluationContextLocalInformation localInformation) {
        final Value iteratorValue = self.access(new Value("iterator"));
        final Value iterator = evaluateFunction(iteratorValue, Collections.singletonList(self), globalContext, localInformation, "iterator");

        if (!iterator.getType().equals(PrimitiveValueType.ITERATOR.getType())) {
            throw localInformation.createException("Iterator element did not provide iterable: " + self);
        }
        @SuppressWarnings("unchecked") final Iterator<Value> iteratorIterator = (Iterator<Value>) iterator.getValue();

        Value result = Value.empty();

        while (iteratorIterator.hasNext()) {
            final EvaluationContextLocalInformation loopLocalInformation = localInformation.deriveNewContext();

            final Object nextObject = iteratorIterator.next();
            if (nextObject == null) {
                throw localInformation.createException("Object returned by iterator is null");
            } else if (!(nextObject instanceof Value)) { // this condition is not always false, no matter what your IDE tells you.
                throw localInformation.createException("Object returned by iterator is not a Value, but a [" + nextObject.getClass().getSimpleName() + "]: " + nextObject);
            }

            final Value iteratorElement = (Value) nextObject;
            if (iteratorElement.isMapAnArray() && iteratorElement.getMap().size() == 2) {
                final LinkedHashMap<Object, Value> inputParameterValues = iteratorElement.getMap();
                final Iterator<Value> parameterElementIterator = inputParameterValues.values().iterator();
                final Value key = parameterElementIterator.next();
                final Value value = parameterElementIterator.next();

                try {
                    result = evaluateFunction(evaluatorFunction, Collections.singletonList(value), globalContext, loopLocalInformation, "forEach");
                } catch (Exception e) {
                    result = evaluateFunction(evaluatorFunction, Arrays.asList(key, value), globalContext, loopLocalInformation, "forEach");
                }

            } else {
                result = evaluateFunction(evaluatorFunction, Collections.singletonList(iteratorElement), globalContext, loopLocalInformation, "forEach");
            }

            if (result.unwrapBreak()) {
                break;
            }
            result.unwrapContinue();
        }

        return result;
    }

    public Value whileLoop(ParserNode originNode, GlobalContext globalContext, SymbolCreationMode symbolCreationMode, EvaluationContextLocalInformation localInformation) {
        final Object conditionNode = originNode.getChildren().get(0);
        final Object loopCode = originNode.getChildren().get(1);

        final EvaluationContextLocalInformation loopLocalInformation = localInformation.deriveNewContext();
        Value result = Value.empty();

        while (true) {
            final Value condition = evaluate(conditionNode, globalContext, symbolCreationMode, loopLocalInformation);
            if (!condition.getType().equals(PrimitiveValueType.BOOLEAN.getType())) {
                throw localInformation.createException("While condition is not a boolean: " + condition);
            }
            if (!condition.isTrue()) {
                break;
            }

            result = evaluate(loopCode, globalContext, symbolCreationMode, loopLocalInformation);

            if (result.unwrapBreak()) {
                break;
            }
            result.unwrapContinue();
        }

        return result;
    }

    private void breakpointReached(EvaluationContextLocalInformation localInformation, ParserNode node) {
        MenterDebugger.haltOnEveryExecutionStep = true;
        while (true) {
            MenterDebugger.printer.print(">>> " + node.reconstructCode() + " [stack, symbols, resume]");

            final int action = MenterDebugger.waitForDebuggerResume();

            if (action == 0) {
                break;
            } else if (action == 1) {
                final StringBuilder sb = new StringBuilder();
                localInformation.appendStackTraceSymbols(sb, new MenterStackTraceElement(null, node), true);
                MenterDebugger.printer.println("Symbols:" + sb);
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
        }
    }

    public Value evaluateFunction(String originalFunctionName, Value functionValue, GlobalContext globalContext, EvaluationContextLocalInformation localInformation, List<Value> functionParameters) {
        return evaluateFunction(functionValue, functionParameters, globalContext, localInformation, originalFunctionName);
    }

    public Value evaluateFunction(String originalFunctionName, Value functionValue, GlobalContext globalContext, EvaluationContextLocalInformation localInformation, Value... functionParameters) {
        return evaluateFunction(functionValue, Arrays.asList(functionParameters), globalContext, localInformation, originalFunctionName);
    }

    public Value evaluateFunction(Value functionValue, List<Value> functionParameters, GlobalContext globalContext, EvaluationContextLocalInformation localInformation, String originalFunctionName) {
        return evaluateFunction(functionValue, functionParameters, globalContext, localInformation, originalFunctionName, null);
    }

    public Value evaluateFunction(Value functionValue, List<Value> functionParameters,
                                  GlobalContext globalContext, EvaluationContextLocalInformation localInformation,
                                  String originalFunctionName,
                                  BiConsumer<EvaluationContextLocalInformation, Map<String, Value>> functionContextTransformer) {
        try {
            localInformation.provideFunctionNameForNextStackTraceElement(originalFunctionName);

            if (!functionValue.isFunction()) {
                throw localInformation.createException("Value is not a function [" + functionValue + "]");
            }

            if (MenterDebugger.logInterpreterFunctionCalls) {
                LOG.info("Calling function [{}] with parameters {}", originalFunctionName, functionParameters);
            }

            final GlobalContext parentClosureContext = functionValue.getTagParentFunctionClosureContext();
            final GlobalContext effectiveParentClosureContext = parentClosureContext != null ? parentClosureContext : globalContext;

            final Value result;
            if (functionValue.getValue() instanceof MenterNodeFunction) {
                final MenterNodeFunction executableFunction = (MenterNodeFunction) functionValue.getValue();
                final List<String> functionArgumentNames = executableFunction.getArgumentNames();

                if (functionArgumentNames.size() != functionParameters.size()) {
                    throw localInformation.createException("Function [" + functionValue + "] requires " + functionArgumentNames.size() + " arguments, but " + functionParameters.size() + " were given");
                }

                final EvaluationContextLocalInformation functionLocalInformation = localInformation.deriveNewFunctionContext();

                final Map<String, Value> argumentValues = new HashMap<>();
                for (int i = 0; i < functionArgumentNames.size(); i++) {
                    final String argumentName = functionArgumentNames.get(i);
                    final Value argumentValue = functionParameters.get(i);

                    argumentValues.put(argumentName, argumentValue);
                }

                if (functionContextTransformer != null) {
                    functionContextTransformer.accept(functionLocalInformation, argumentValues);
                }

                functionLocalInformation.putLocalSymbol(executableFunction.getParentContext().getVariables());

                final EvaluationContextLocalInformation parentClosureLocalSymbols = functionValue.getTagParentFunctionClosureLocalInformation();
                if (parentClosureLocalSymbols != null) {
                    functionLocalInformation.putLocalSymbol(parentClosureLocalSymbols);
                }

                for (Map.Entry<String, Value> args : argumentValues.entrySet()) {
                    functionLocalInformation.putLocalSymbol(args.getKey(), args.getValue());
                }

                result = evaluate(executableFunction.getBody(), effectiveParentClosureContext, SymbolCreationMode.THROW_IF_NOT_EXISTS, functionLocalInformation);

            } else if (Objects.equals(functionValue.getType(), PrimitiveValueType.NATIVE_FUNCTION.getType())) {
                final NativeFunction nativeFunction;
                try {
                    nativeFunction = (NativeFunction) functionValue.getValue();
                } catch (Exception e) {
                    throw localInformation.createException("Native function [" + originalFunctionName + "] does not have the correct signature; must be List<Value> -> Value");
                }
                result = nativeFunction.execute(effectiveParentClosureContext, localInformation, functionParameters);

            } else if (Objects.equals(functionValue.getType(), PrimitiveValueType.REFLECTIVE_FUNCTION.getType())) {
                final Method executableFunction = (Method) functionValue.getValue();
                final Value calledOnValue = functionValue.getTagParentFunctionValue();
                final CustomType calledOnCustomType = (CustomType) calledOnValue.getValue();

                if (calledOnCustomType != null) {
                    result = calledOnCustomType.callReflectiveMethod(executableFunction, functionParameters);
                } else {
                    // static method
                    result = (Value) executableFunction.invoke(null, functionParameters);
                }

            } else { // otherwise it must be a value function
                final MenterValueFunction executableFunction = (MenterValueFunction) functionValue.getValue();
                final Value executeOnValue = functionValue.getTagParentFunctionValue();
                if (executeOnValue == null) {
                    throw localInformation.createException("Function [" + originalFunctionName + "] cannot be called without a base value to execute on");
                }
                result = executableFunction.apply(effectiveParentClosureContext, executeOnValue, functionParameters, localInformation);
            }

            result.unwrapReturn();
            return result;
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

    private boolean isAssignmentTargetFunctionCall(Object o) {
        if (Parser.isType(o, ParserNode.NodeType.FUNCTION_CALL)) {
            return false;
        } else if (Parser.isType(o, ParserNode.NodeType.IDENTIFIER_ACCESSED)) {
            // last child must not be a function call or parenthesis pair
            final ParserNode identifierAccessedNode = (ParserNode) o;
            final List<Object> children = identifierAccessedNode.getChildren();
            if (children.isEmpty()) {
                return true;
            }
            final Object lastChild = children.get(children.size() - 1);
            return !Parser.isType(lastChild, ParserNode.NodeType.FUNCTION_CALL) && !Parser.isType(lastChild, ParserNode.NodeType.PARENTHESIS_PAIR);
        }
        return true;
    }

    enum SymbolCreationMode {
        CREATE_IF_NOT_EXISTS,
        THROW_IF_NOT_EXISTS,
        CREATE_NEW_ANYWAYS
    }

    private Value resolveSymbol(Object identifier, SymbolCreationMode symbolCreationMode, GlobalContext globalContext, EvaluationContextLocalInformation localInformation) {
        if (MenterDebugger.logInterpreterResolveSymbols) {
            MenterDebugger.printer.println("Symbol resolve start: " + ParserNode.reconstructCode(identifier));
        }

        final boolean symbolCreationModeIsAllowedToCreateVariable = SymbolCreationMode.CREATE_IF_NOT_EXISTS.equals(symbolCreationMode) || SymbolCreationMode.CREATE_NEW_ANYWAYS.equals(symbolCreationMode);

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
            if (token.getType() == TokenType.IDENTIFIER) {
                identifiers.add(token);
            }
        } else if (identifier instanceof Value || identifier instanceof String) {
            identifiers.add(identifier);
        }

        // handle code blocks/expressions
        for (int i = 0; i < identifiers.size(); i++) {
            if (identifiers.get(i) instanceof ParserNode) {
                final ParserNode node = (ParserNode) identifiers.get(i);
                if (node.getType() == ParserNode.NodeType.CODE_BLOCK || node.getType() == ParserNode.NodeType.EXPRESSION
                    || node.getType() == ParserNode.NodeType.IDENTIFIER_ACCESSED) {
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
            MenterDebugger.printer.println("Symbol resolve: Split into identifiers: " + identifiers.stream().map(ParserNode::reconstructCode).collect(Collectors.joining(" --> ")));
        }

        final GlobalContext originalGlobalContext = globalContext;
        Module switchedModule = null;
        Value value = null;
        Value selfMapValue = null;

        for (int i = 0; i < identifiers.size(); i++) {
            final Object id = identifiers.get(i);
            final String stringKey = Module.ID_TO_KEY_MAPPER.apply(id);

            if (MenterDebugger.logInterpreterResolveSymbols) {
                MenterDebugger.printer.printf("Symbol resolve step: %d (%s): %s%n", i, stringKey, ParserNode.reconstructCode(id));
            }

            final boolean isFinalIdentifier = id == identifiers.get(identifiers.size() - 1);
            final Value previousValue = value;

            if (PrimitiveValueType.isType(value, PrimitiveValueType.OBJECT)) {
                selfMapValue = value;
            }

            if (value == null) {
                if (localInformation.hasLocalSymbol(stringKey)) {
                    if (switchedModule != null && !switchedModule.containsSymbol(stringKey)) {
                        throw localInformation.createException("Illegal access on [" + switchedModule.getName() + "." + stringKey + "]: module does not export symbol");
                    }

                    if (SymbolCreationMode.CREATE_NEW_ANYWAYS.equals(symbolCreationMode)) {
                        value = new Value(localInformation.getLocalSymbol(stringKey).getValue());
                        localInformation.putLocalSymbol(stringKey, value);
                    } else {
                        value = localInformation.getLocalSymbol(stringKey);
                    }

                    if (MenterDebugger.logInterpreterResolveSymbols) {
                        MenterDebugger.printer.printf("Symbol resolve: [%s] from local symbols is [%s]%n", stringKey, value);
                    }
                    continue;
                }

                final Value variable = globalContext.getVariable(stringKey);
                if (variable != null) {
                    if (SymbolCreationMode.CREATE_NEW_ANYWAYS.equals(symbolCreationMode)) {
                        value = new Value(variable.getValue());
                        globalContext.addVariable(stringKey, value);
                    } else {
                        value = variable;
                    }

                    if (MenterDebugger.logInterpreterResolveSymbols) {
                        MenterDebugger.printer.println("Symbol resolve: [" + stringKey + "] from global variables");
                    }
                    continue;
                }

                boolean foundImport = false;
                for (Import anImport : globalContext.getImports()) {
                    if (anImport.getModule() == null) {
                        throw new MenterExecutionException("Module has not finished loading yet: " + anImport + "\nTo finish loading, call the finishLoadingContexts() method on the MenterInterpreter instance.\nNOTE: This is most likely a bug in the MenterInterpreter implementation.");
                    }

                    if (!anImport.isInline() && anImport.getAliasOrName().equals(stringKey)) {
                        if (originalGlobalContext != globalContext) {
                            throw localInformation.createException("Illegal access on [" + ParserNode.reconstructCode(identifier) + "]: Cannot access reference to the [" + anImport.getModule().getName() + "] module.\n"
                                                                   + "Accessing symbols across modules is disallowed.");
                        }

                        final Module module = anImport.getModule();
                        if (module != null) {
                            globalContext = module.getParentContext();
                            switchedModule = module;
                            localInformation = localInformation.deriveNewContext();
                            localInformation.putLocalSymbol(globalContext.getVariables());
                            // value = null; // is already null
                            foundImport = true;

                            if (MenterDebugger.logInterpreterResolveSymbols) {
                                MenterDebugger.printer.println("Symbol resolve: [" + stringKey + "] from import: " + anImport + "; switching to module context");
                            }
                            break;
                        }
                    } else if (anImport.isInline() && anImport.getModule().containsSymbol(stringKey)) {
                        if (originalGlobalContext != globalContext) {
                            throw localInformation.createException("Illegal access on [" + ParserNode.reconstructCode(identifier) + "]: [" + stringKey + "] references a symbol from the [" + anImport.getModule().getName() + "] module.\n"
                                                                   + "Accessing symbols across modules is disallowed.");
                        }

                        final Module module = anImport.getModule();
                        if (module != null) {
                            globalContext = module.getParentContext();
                            switchedModule = module;
                            localInformation = localInformation.deriveNewContext();
                            localInformation.putLocalSymbol(globalContext.getVariables());
                            value = module.getParentContext().getVariable(stringKey);
                            foundImport = true;

                            if (MenterDebugger.logInterpreterResolveSymbols) {
                                MenterDebugger.printer.printf("Symbol resolve: [%s] from inline import: %s; switching to module context%n", stringKey, anImport);
                            }
                            break;
                        }
                    }
                }
                if (foundImport) continue;

                if (id instanceof Value) {
                    value = (Value) id;
                    if (MenterDebugger.logInterpreterResolveSymbols) {
                        MenterDebugger.printer.printf("Symbol resolve: [%s] from value%n", stringKey);
                    }
                    continue;
                }

                if (id instanceof ParserNode) {
                    final ParserNode node = (ParserNode) id;
                    if (Parser.isEvaluableToValue(node)) {
                        value = evaluate(node, globalContext, symbolCreationMode, localInformation);
                        if (MenterDebugger.logInterpreterResolveSymbols) {
                            MenterDebugger.printer.printf("Symbol resolve: [%s] from evaluated node%n", value);
                        }
                        continue;
                    }
                }

                if ("symbols".equals(stringKey) && globalContext != originalGlobalContext) {
                    value = new Value(globalContext.getVariables());
                    if (MenterDebugger.logInterpreterResolveSymbols) {
                        MenterDebugger.printer.printf("Symbol resolve: [%s] from symbols%n", value);
                    }
                    continue;
                }

            } else {
                if (Parser.isType(id, ParserNode.NodeType.FUNCTION_CALL)) {
                    final List<Value> functionParameters = makeFunctionArguments(id, originalGlobalContext, localInformation);
                    try {
                        final Value finalSelfMapValue = selfMapValue;
                        value = evaluateFunction(value, functionParameters, globalContext, localInformation, ParserNode.reconstructCode(identifiers.get(i - 1)),
                                (functionContext, args) -> {
                                    if (finalSelfMapValue != null) {
                                        functionContext.putSelf(finalSelfMapValue);
                                    }
                                });

                        if (MenterDebugger.logInterpreterResolveSymbols) {
                            MenterDebugger.printer.format("Symbol resolve: [%s] from function call", value);
                        }

                    } catch (Exception e) {
                        throw localInformation.createException(e.getMessage() + ": " + ParserNode.reconstructCode(identifiers.get(i - 1)), e);
                    }
                    continue;

                } else {
                    final Value accessAs = id instanceof Value ? (Value) id : new Value(getTokenOrNodeValue(id));
                    try {
                        value = value.access(accessAs);
                    } catch (Exception e) {
                        value = null;
                    }

                    if (value != null) {
                        if (MenterDebugger.logInterpreterResolveSymbols) {
                            MenterDebugger.printer.format("Symbol resolve: [%s] from accessing previous value: %s", stringKey, previousValue);
                        }
                        continue;

                    } else if (symbolCreationModeIsAllowedToCreateVariable) {
                        value = Value.empty();
                        if (!previousValue.create(accessAs, value, isFinalIdentifier)) {
                            value = null;
                        } else if (MenterDebugger.logInterpreterResolveSymbols) {
                            MenterDebugger.printer.format("Symbol resolve: [%s] from creating new value on previous value: %s", stringKey, previousValue);
                        }
                        continue;
                    }
                }
            }

            if (SymbolCreationMode.THROW_IF_NOT_EXISTS.equals(symbolCreationMode)) {
                final List<String> candidates = findMostLikelyCandidates(globalContext, previousValue, stringKey);

                throw localInformation.createException("Cannot resolve symbol '" + stringKey + "' on [" + ParserNode.reconstructCode(identifier) + "]" +
                                                       (previousValue != null ? "\nEvaluation stopped at value: " + ParserNode.reconstructCode(previousValue) : "") +
                                                       (candidates.isEmpty() ? "" : "\nDid you mean " + candidates.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ")) + "?"));

            } else if (symbolCreationModeIsAllowedToCreateVariable) {
                if (isFinalIdentifier) {
                    value = Value.empty();
                } else {
                    value = new Value(new LinkedHashMap<>());
                }

                localInformation.putLocalSymbol(stringKey, value);
                if (MenterDebugger.logInterpreterResolveSymbols) {
                    MenterDebugger.printer.format("Symbol resolve: [%s] from creating new value", stringKey);
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

            if (previousValue.getValue() instanceof CustomType) {
                candidates.addAll(((CustomType) previousValue.getValue()).findReflectiveMethodNames());
            }

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
        return IntStream.range(1, localInformation.getStackTrace().size()).mapToObj(x -> VERTICAL_LINE + SPACE).collect(Collectors.joining());
    }

    private final static String VERTICAL_LINE = "│";
    private final static String HORIZONTAL_LINE = "─";
    private final static String CORNER = "└";
    private final static String ARROW_HEAD = ">";
    private final static String SPACE = " ";
}
