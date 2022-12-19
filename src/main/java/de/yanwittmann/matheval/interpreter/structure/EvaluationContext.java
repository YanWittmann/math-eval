package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.interpreter.MenterDebugger;
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

public abstract class EvaluationContext {

    private static final Logger LOG = LogManager.getLogger(EvaluationContext.class);

    private final EvaluationContext parentContext;
    private final Map<String, Value> variables;
    protected final static Map<String[], Function<Value[], Value>> nativeFunctions = new HashMap<>();

    static {
        nativeFunctions.put(new String[]{"core.ter", "print"}, arguments -> {
            System.out.println(Arrays.stream(arguments)
                    .map(v -> v.toDisplayString())
                    .collect(Collectors.joining(" ")));
            return Value.empty();
        });
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
                throw new MenterExecutionException("Import statements are not supported in the interpreter.");

            } else if (node.getType() == ParserNode.NodeType.ARRAY) {
                final List<Value> array = new ArrayList<>();
                for (Object child : node.getChildren()) {
                    array.add(evaluate(child, globalContext, localSymbols, symbolCreationMode));
                }
                result = new Value(array);

            } else if (node.getType() == ParserNode.NodeType.MAP) {
                final Map<String, Value> map = new HashMap<>();
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
                        throw new MenterExecutionException("Invalid map key type: " + keyNode.getClass().getName());
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
                        throw new MenterExecutionException("Operator " + op.getSymbol() + " requires " + numberOfArguments + " arguments, but " + numberOfChildren + " were given:", node);
                    }

                    final Value[] arguments = new Value[numberOfArguments];
                    for (int i = 0; i < numberOfArguments; i++) {
                        arguments[i] = evaluate(node.getChildren().get(i), globalContext, localSymbols, symbolCreationMode);
                    }

                    result = op.evaluate(arguments);
                    if (result == null) {
                        throw new MenterExecutionException("Operator " + op.getSymbol() + " returned null as result; this is most likely due to an incomplete implementation of the operator.", node);
                    }
                }
            } else if (node.getType() == ParserNode.NodeType.IDENTIFIER_ACCESSED) {
                result = resolveSymbol(node, symbolCreationMode, globalContext, localSymbols);

            } else if (node.getType() == ParserNode.NodeType.ASSIGNMENT) {
                final Value variable = evaluate(node.getChildren().get(0), globalContext, localSymbols, SymbolCreationMode.CREATE_IF_NOT_EXISTS);
                final Value value = evaluate(node.getChildren().get(1), globalContext, new HashMap<>(localSymbols), SymbolCreationMode.THROW_IF_NOT_EXISTS);

                variable.inheritValue(value);
                result = value;

            } else if (node.getType() == ParserNode.NodeType.PARENTHESIS_PAIR) {
                result = new Value(node.getChildren().stream()
                        .map(child -> evaluate(child, globalContext, localSymbols, symbolCreationMode))
                        .collect(Collectors.toList()));

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_DECLARATION) {
                if (Parser.isKeyword(node.getChildren().get(0), "native")) {
                    final Value functionValue = evaluate(node.getChildren().get(1), globalContext, localSymbols, SymbolCreationMode.CREATE_IF_NOT_EXISTS);
                    final String functionName = ((Token) node.getChildren().get(1)).getValue();
                    final List<Object> functionArguments = Parser.isType(node.getChildren().get(2), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(2)).getChildren() : null;
                    if (functionArguments == null) {
                        throw new MenterExecutionException("Function arguments are not a parenthesis pair", node);
                    }

                    if (!(this instanceof GlobalContext)) {
                        throw new MenterExecutionException("Native functions can only be declared in the global context", node);
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
                        throw new MenterExecutionException("Native function [" + functionName + "] not found using candidates: " + moduleNameCandidates, node);
                    }

                } else {
                    final Value functionValue = evaluate(node.getChildren().get(0), globalContext, localSymbols, SymbolCreationMode.CREATE_IF_NOT_EXISTS);
                    final List<Object> functionArguments = Parser.isType(node.getChildren().get(1), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(1)).getChildren() : null;
                    if (functionArguments == null) {
                        throw new MenterExecutionException("Function arguments are not a parenthesis pair", node);
                    }
                    final ParserNode functionCode = (ParserNode) node.getChildren().get(2);

                    final MFunction function = new MFunction(globalContext, functionArguments, functionCode);
                    functionValue.setValue(function);

                    result = functionValue;
                }

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_INLINE) {
                // 0 is params, 1 is body
                final List<Object> functionArguments = Parser.isType(node.getChildren().get(0), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(0)).getChildren() : null;
                if (functionArguments == null) {
                    throw new MenterExecutionException("Function arguments are not a parenthesis pair", node);
                }

                final ParserNode functionCode = (ParserNode) node.getChildren().get(1);
                final MFunction function = new MFunction(globalContext, functionArguments, functionCode);
                result = new Value(function);

            } else if (node.getType() == ParserNode.NodeType.FUNCTION_CALL) {
                final Value function = evaluate(node.getChildren().get(0), globalContext, localSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS);

                if (!function.isFunction()) {
                    throw new MenterExecutionException("Value is not a function " + function, node);
                }

                final List<Object> functionArguments = Parser.isType(node.getChildren().get(1), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(1)).getChildren() : null;
                if (functionArguments == null) {
                    throw new MenterExecutionException("Function arguments are not a parenthesis pair", node);
                }
                final List<Value> evaluatedArguments = functionArguments.stream()
                        .map(argument -> evaluate(argument, globalContext, localSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS))
                        .collect(Collectors.toList());

                if (function.getValue() instanceof MFunction) {
                    final MFunction executableFunction = (MFunction) function.getValue();
                    final List<String> functionArgumentNames = executableFunction.getArgumentNames();

                    if (functionArgumentNames.size() != evaluatedArguments.size()) {
                        throw new MenterExecutionException("Function " + function + " requires " + functionArgumentNames.size() + " arguments, but " + evaluatedArguments.size() + " were given", node);
                    }

                    final Map<String, Value> functionLocalSymbols = new HashMap<>(localSymbols);
                    for (int i = 0; i < functionArgumentNames.size(); i++) {
                        final String argumentName = functionArgumentNames.get(i);
                        final Value argumentValue = evaluatedArguments.get(i);
                        functionLocalSymbols.put(argumentName, argumentValue);
                    }

                    functionLocalSymbols.putAll(executableFunction.getParentContext().getVariables());

                    result = evaluate(executableFunction.getBody(), globalContext, functionLocalSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS);

                } else if (Objects.equals(function.getType(), PrimitiveValueType.NATIVE_FUNCTION.getType())) {
                    final Function<Value[], Value> nativeFunction = (Function<Value[], Value>) function.getValue();
                    final Value[] nativeFunctionArguments = evaluatedArguments.toArray(new Value[0]);
                    result = nativeFunction.apply(nativeFunctionArguments);

                } else {
                    final BiFunction<Value, List<Value>, Value> executableFunction = (BiFunction<Value, List<Value>, Value>) function.getValue();
                    result = executableFunction.apply(function.getSecondaryValue(), evaluatedArguments);
                }
            }

            if (result == null) {
                throw new MenterExecutionException("Node did not evaluate to anything:", node);
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

    enum SymbolCreationMode {
        CREATE_IF_NOT_EXISTS,
        THROW_IF_NOT_EXISTS
    }

    private Value resolveSymbol(Object identifier, SymbolCreationMode symbolCreationMode, GlobalContext globalContext, Map<String, Value> localSymbols) {

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
            throw new MenterExecutionException("Cannot resolve symbol from " + identifier);
        }

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
                    continue;
                }

                final Value variable = this.getVariable(stringKey);
                if (variable != null) {
                    value = variable;
                    continue;
                }

                boolean foundImport = false;
                for (Import anImport : globalContext.getImports()) {
                    if (anImport.getAliasOrName().equals(stringKey) && anImport.getModule().containsSymbol(nextStringKey)) {
                        final Module module = anImport.getModule();
                        if (module != null) {
                            globalContext = module.getParentContext();
                            localSymbols = globalContext.getVariables();
                            value = null;
                            foundImport = true;
                            break;
                        }
                    } else if (anImport.isInline() && anImport.getModule().containsSymbol(stringKey)) {
                        final Module module = anImport.getModule();
                        if (module != null) {
                            globalContext = module.getParentContext();
                            localSymbols = globalContext.getVariables();
                            value = module.getParentContext().getVariable(stringKey);
                            foundImport = true;
                            break;
                        }
                    }
                }
                if (foundImport) continue;

            } else {
                final Value accessAs = id instanceof Value ? (Value) id : new Value(getTokenOrNodeValue(id));
                value = value.access(accessAs);
                if (value != null) {
                    continue;
                } else if (SymbolCreationMode.CREATE_IF_NOT_EXISTS.equals(symbolCreationMode)) {
                    value = previousValue.create(accessAs, Value.empty());
                    continue;
                }
            }

            if (SymbolCreationMode.THROW_IF_NOT_EXISTS == symbolCreationMode) {
                throw new MenterExecutionException("Cannot resolve symbol '" + stringKey + "' on\n" + identifier);
            } else if (SymbolCreationMode.CREATE_IF_NOT_EXISTS == symbolCreationMode) {
                if (isFinalIdentifier) {
                    value = Value.empty();
                } else {
                    value = new Value(new HashMap<>());
                }
                localSymbols.put(stringKey, value);
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
