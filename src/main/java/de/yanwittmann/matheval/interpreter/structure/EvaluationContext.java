package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.interpreter.MenterDebugger;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.operator.Operator;
import de.yanwittmann.matheval.parser.Parser;
import de.yanwittmann.matheval.parser.ParserNode;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class EvaluationContext {

    private static final Logger LOG = LogManager.getLogger(EvaluationContext.class);

    private final EvaluationContext parentContext;
    private final Map<String, Value> variables;
    protected final static Map<String[], Function<Value[], Value>> nativeFunctions = new HashMap<>();

    static {
        nativeFunctions.put(new String[]{"common.ter", "print"}, arguments -> {
            System.out.println(Arrays.stream(arguments)
                    .map(v -> v.toDisplayString())
                    .collect(Collectors.joining(" ")));
            return Value.empty();
        });

        nativeFunctions.put(new String[]{"io.ter", "read"}, arguments -> {
            try {
                return new Value(FileUtils.readLines(new File(arguments[0].getValue().toString()), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new MenterExecutionException("Could not read file '" + arguments[0].toString() + "'.");
            }
        });

        nativeFunctions.put(new String[]{"system.ter", "getProperty"}, arguments -> new Value(System.getProperty(arguments[0].toString())));
        nativeFunctions.put(new String[]{"system.ter", "getEnv"}, arguments -> new Value(System.getenv(arguments[0].toString())));
    }

    public static void putNativeFunction(String[] path, Function<Value[], Value> function) {
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

    public Value evaluate(Object nodeOrToken, GlobalContext globalContext, Map<String, Value> localSymbols, SymbolCreationMode symbolCreationMode) {
        Value result = null;

        if (nodeOrToken instanceof ParserNode) {
            final ParserNode node = (ParserNode) nodeOrToken;

            if (MenterDebugger.logInterpreterEvaluation) {
                LOG.info(node.reconstructCode());
            }
            if (MenterDebugger.breakpointActivationCode != null && node.reconstructCode().equals(MenterDebugger.breakpointActivationCode)) {
                LOG.info("Found debugger breakpoint, place a breakpoint here to debug: {}.java:{}\nBreakpoint code: {}", EvaluationContext.class.getName(), new Throwable().getStackTrace()[0].getLineNumber(), node.reconstructCode());
            }

            if (node.getType() == ParserNode.NodeType.STATEMENT || node.getType() == ParserNode.NodeType.ROOT || node.getType() == ParserNode.NodeType.CODE_BLOCK) {
                for (Object child : node.getChildren()) {
                    result = evaluate(child, globalContext, localSymbols, symbolCreationMode);
                    if (child instanceof ParserNode && ((ParserNode) child).getType() == ParserNode.NodeType.RETURN_STATEMENT) {
                        break;
                    }
                }
            } else if (node.getType() == ParserNode.NodeType.RETURN_STATEMENT) {
                result = evaluate(node.getChildren().get(0), globalContext, localSymbols, symbolCreationMode);

            } else if (node.getType() == ParserNode.NodeType.IMPORT_STATEMENT) {
                throw new MenterExecutionException(globalContext, "Import statements are not supported in the interpreter.");

            } else if (node.getType() == ParserNode.NodeType.ARRAY) {
                final Map<Object, Value> array = new LinkedHashMap<>();
                final List<Object> children = node.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    array.put(new BigDecimal(i), evaluate(children.get(i), globalContext, localSymbols, symbolCreationMode));
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
                        key = evaluate(keyNode, globalContext, localSymbols, symbolCreationMode).toDisplayString();
                    } else {
                        throw new MenterExecutionException(globalContext, "Invalid map key type: " + keyNode.getClass().getName());
                    }

                    final Value value = evaluate(childNode.getChildren().get(1), globalContext, localSymbols, symbolCreationMode);
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
                        throw new MenterExecutionException(globalContext, "Operator " + op.getSymbol() + " requires " + numberOfArguments + " arguments, but " + numberOfChildren + " were given:", node);
                    }

                    final Value[] arguments = new Value[numberOfArguments];
                    for (int i = 0; i < numberOfArguments; i++) {
                        arguments[i] = evaluate(node.getChildren().get(i), globalContext, localSymbols, symbolCreationMode);
                    }

                    result = op.evaluate(arguments);
                    if (result == null) {
                        throw new MenterExecutionException(globalContext, "Operator " + op.getSymbol() + " returned null as result; this is most likely due to an incomplete implementation of the operator.", node);
                    }
                }
            } else if (node.getType() == ParserNode.NodeType.IDENTIFIER_ACCESSED) {
                result = resolveSymbol(node, symbolCreationMode, globalContext, localSymbols);

            } else if (node.getType() == ParserNode.NodeType.ASSIGNMENT) {
                final Value variable = evaluate(node.getChildren().get(0), globalContext, localSymbols, SymbolCreationMode.CREATE_IF_NOT_EXISTS);
                final Value value = evaluate(node.getChildren().get(1), globalContext, new HashMap<>(localSymbols), SymbolCreationMode.THROW_IF_NOT_EXISTS);

                variable.inheritValue(value);
                result = value;

                if (MenterDebugger.logInterpreterAssignments) {
                    LOG.info("Assigned value [{}] to variable [{}] from: {}", value, ParserNode.reconstructCode(node.getChildren().get(0)), node.reconstructCode());
                }

            } else if (node.getType() == ParserNode.NodeType.PARENTHESIS_PAIR) {
                if (node.getChildren().size() == 1) {
                    result = evaluate(node.getChildren().get(0), globalContext, localSymbols, symbolCreationMode);
                } else {
                    result = new Value(node.getChildren().stream()
                            .map(child -> evaluate(child, globalContext, localSymbols, symbolCreationMode))
                            .collect(Collectors.toList()));
                }

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_DECLARATION) {
                if (Parser.isKeyword(node.getChildren().get(0), "native")) {
                    final Value functionValue = evaluate(node.getChildren().get(1), globalContext, localSymbols, SymbolCreationMode.CREATE_IF_NOT_EXISTS);
                    final String functionName = ((Token) node.getChildren().get(1)).getValue();
                    final List<Object> functionArguments = Parser.isType(node.getChildren().get(2), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(2)).getChildren() : null;
                    if (functionArguments == null) {
                        throw new MenterExecutionException(globalContext, "Function arguments are not a parenthesis pair", node);
                    }

                    if (!(this instanceof GlobalContext)) {
                        throw new MenterExecutionException(globalContext, "Native functions can only be declared in the global context", node);
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
                        throw new MenterExecutionException(globalContext, "Native function [" + functionName + "] not found using candidates: " + moduleNameCandidates, node);
                    }

                } else {
                    final Value functionValue = evaluate(node.getChildren().get(0), globalContext, localSymbols, SymbolCreationMode.CREATE_IF_NOT_EXISTS);
                    final List<Object> functionArguments = Parser.isType(node.getChildren().get(1), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(1)).getChildren() : null;
                    if (functionArguments == null) {
                        throw new MenterExecutionException(globalContext, "Function arguments are not a parenthesis pair", node);
                    }
                    final ParserNode functionCode = (ParserNode) node.getChildren().get(2);

                    final MNodeFunction function = new MNodeFunction(globalContext, functionArguments, functionCode);
                    functionValue.setValue(function);

                    result = functionValue;
                }

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_INLINE) {
                // 0 is params, 1 is body
                final List<Object> functionArguments = Parser.isType(node.getChildren().get(0), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(0)).getChildren() : null;
                if (functionArguments == null) {
                    throw new MenterExecutionException(globalContext, "Function arguments are not a parenthesis pair", node);
                }

                final ParserNode functionCode = (ParserNode) node.getChildren().get(1);
                final MNodeFunction function = new MNodeFunction(globalContext, functionArguments, functionCode);
                result = new Value(function);

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_CALL) {
                final Value function = evaluate(node.getChildren().get(0), globalContext, localSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS);
                if (!Parser.isType(node.getChildren().get(1), ParserNode.NodeType.PARENTHESIS_PAIR)) {
                    throw new MenterExecutionException(globalContext, "Function arguments are not a parenthesis pair", node);
                }

                final List<Value> functionParameters = makeFunctionArguments(node, globalContext, localSymbols);
                result = evaluateFunction(function, functionParameters, globalContext, localSymbols);

            } else if (node.getType() == ParserNode.NodeType.CONDITIONAL) {
                final List<ParserNode> branches = node.getChildren().stream()
                        .filter(child -> Parser.isType(child, ParserNode.NodeType.CONDITIONAL_BRANCH))
                        .map(c -> (ParserNode) c)
                        .collect(Collectors.toList());

                boolean foundMatchingBranch = false;
                for (ParserNode branch : branches) {
                    if (branch.getChildren().size() == 1) { // else
                        result = evaluate(branch.getChildren().get(0), globalContext, localSymbols, symbolCreationMode);
                        foundMatchingBranch = true;
                        break;

                    } else {
                        final Value condition = evaluate(branch.getChildren().get(0), globalContext, localSymbols, symbolCreationMode);
                        if (condition.isTrue()) {
                            result = evaluate(branch.getChildren().get(1), globalContext, localSymbols, symbolCreationMode);
                            foundMatchingBranch = true;
                            break;
                        }
                    }
                }

                if (!foundMatchingBranch) {
                    result = Value.empty();
                }
            }

            if (result == null) {
                throw new MenterExecutionException(globalContext, "Node did not evaluate to anything:", node);
            }

        } else if (nodeOrToken instanceof Token) {
            final Token node = (Token) nodeOrToken;

            if (node.getType() == Lexer.TokenType.IDENTIFIER) {
                result = resolveSymbol(node, symbolCreationMode, globalContext, localSymbols);

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

        return result;
    }

    protected Value evaluateFunction(Value functionValue, List<Value> functionParameters, GlobalContext globalContext, Map<String, Value> localSymbols) {
        if (!functionValue.isFunction()) {
            throw new MenterExecutionException(globalContext, "Value is not a function " + functionValue);
        }

        if (functionValue.getValue() instanceof MNodeFunction) {
            final MNodeFunction executableFunction = (MNodeFunction) functionValue.getValue();
            final List<String> functionArgumentNames = executableFunction.getArgumentNames();

            if (functionArgumentNames.size() != functionParameters.size()) {
                throw new MenterExecutionException(globalContext, "Function " + functionValue + " requires " + functionArgumentNames.size() + " arguments, but " + functionParameters.size() + " were given");
            }

            final Map<String, Value> functionLocalSymbols = new HashMap<>(localSymbols);
            for (int i = 0; i < functionArgumentNames.size(); i++) {
                final String argumentName = functionArgumentNames.get(i);
                final Value argumentValue = functionParameters.get(i);
                functionLocalSymbols.put(argumentName, argumentValue);
            }

            functionLocalSymbols.putAll(executableFunction.getParentContext().getVariables());

            return evaluate(executableFunction.getBody(), globalContext, functionLocalSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS);

        } else if (Objects.equals(functionValue.getType(), PrimitiveValueType.NATIVE_FUNCTION.getType())) { // native functions
            final Function<Value[], Value> nativeFunction = (Function<Value[], Value>) functionValue.getValue();
            final Value[] nativeFunctionArguments = functionParameters.toArray(new Value[0]);
            return nativeFunction.apply(nativeFunctionArguments);

        } else { // otherwise it must be a value function
            final MValueFunction executableFunction = (MValueFunction) functionValue.getValue();
            return executableFunction.apply(globalContext, functionValue.getSecondaryValue(), functionParameters, localSymbols);
        }
    }

    private List<Value> makeFunctionArguments(Object functionParameters, GlobalContext parameterGlobalContext, Map<String, Value> localSymbols) {
        if (!(functionParameters instanceof ParserNode)) {
            throw new MenterExecutionException(parameterGlobalContext, "Function parameters are not a parenthesis pair\n" + functionParameters);
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
                    .findFirst()
                    .orElse(null);
        }


        final List<Object> functionArguments = Parser.isType(parenthesisPair, ParserNode.NodeType.PARENTHESIS_PAIR) ? parenthesisPair.getChildren() : null;
        if (functionArguments == null) {
            throw new MenterExecutionException(parameterGlobalContext, "Function arguments are not a parenthesis pair", functionParametersNode);
        }
        return functionArguments.stream()
                .map(argument -> evaluate(argument, parameterGlobalContext, localSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS))
                .collect(Collectors.toList());
    }

    enum SymbolCreationMode {
        CREATE_IF_NOT_EXISTS,
        THROW_IF_NOT_EXISTS
    }

    private Value resolveSymbol(Object identifier, SymbolCreationMode symbolCreationMode, GlobalContext globalContext, Map<String, Value> localSymbols) {
        if (MenterDebugger.logInterpreterResolveSymbols) {
            LOG.info("Symbol resolve start: {}", ParserNode.reconstructCode(identifier));
        }

        final List<Object> identifiers = new ArrayList<>();
        if (identifier instanceof ParserNode) {
            final ParserNode node = (ParserNode) identifier;
            if (node.getType() == ParserNode.NodeType.IDENTIFIER_ACCESSED) {
                identifiers.addAll(node.getChildren());
            } else if (Parser.isLiteral(node)) {
                identifiers.add(evaluate(node, globalContext, localSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS));
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
                    final Value value = evaluate(node, globalContext, new HashMap<>(localSymbols), SymbolCreationMode.THROW_IF_NOT_EXISTS);
                    identifiers.set(i, value);
                }
            } else if (identifiers.get(i) instanceof Token) {
                final Token token = (Token) identifiers.get(i);
                if (Parser.isLiteral(token)) {
                    final Value value = evaluate(token, globalContext, new HashMap<>(localSymbols), SymbolCreationMode.THROW_IF_NOT_EXISTS);
                    identifiers.set(i, value);
                }
            }
        }

        if (identifiers.isEmpty()) {
            throw new MenterExecutionException(globalContext, "Cannot resolve symbol from " + identifier);
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
                if (localSymbols.containsKey(stringKey)) {
                    value = localSymbols.get(stringKey);
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
                    if (anImport.getAliasOrName().equals(stringKey) && anImport.getModule().containsSymbol(nextStringKey)) {
                        final Module module = anImport.getModule();
                        if (module != null) {
                            globalContext = module.getParentContext();
                            localSymbols = globalContext.getVariables();
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
                            localSymbols = globalContext.getVariables();
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
                        value = evaluate(node, globalContext, localSymbols, symbolCreationMode);
                        if (MenterDebugger.logInterpreterResolveSymbols) {
                            LOG.info("Symbol resolve: [{}] from evaluated node", value);
                        }
                        continue;
                    }
                }

            } else {
                if (Parser.isType(id, ParserNode.NodeType.FUNCTION_CALL)) {
                    final List<Value> functionParameters = makeFunctionArguments(id, originalGlobalContext, localSymbols);
                    value = evaluateFunction(value, functionParameters, globalContext, localSymbols);
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
                        if (isFinalIdentifier) {
                            value = Value.empty();
                        } else {
                            value = new Value(new LinkedHashMap<>());
                        }
                        value = previousValue.create(accessAs, value);
                        if (MenterDebugger.logInterpreterResolveSymbols) {
                            LOG.info("Symbol resolve: [{}] from creating new value on previous value: {}", stringKey, previousValue);
                        }
                        continue;
                    }
                }
            }

            if (SymbolCreationMode.THROW_IF_NOT_EXISTS == symbolCreationMode) {
                throw new MenterExecutionException(globalContext, "Cannot resolve symbol '" + stringKey + "' on\n" + ParserNode.reconstructCode(identifier));
            } else if (SymbolCreationMode.CREATE_IF_NOT_EXISTS == symbolCreationMode) {
                if (isFinalIdentifier) {
                    value = Value.empty();
                } else {
                    value = new Value(new LinkedHashMap<>());
                }
                localSymbols.put(stringKey, value);
                if (MenterDebugger.logInterpreterResolveSymbols) {
                    LOG.info("Symbol resolve: [{}] from creating new value", stringKey);
                }
            }
        }

        return value;
    }

    public Object getTokenOrNodeValue(Object node) {
        if (node instanceof Token) {
            return ((Token) node).getValue();
        } else if (node instanceof ParserNode) {
            return ((ParserNode) node).getValue();
        }
        return null;
    }
}
