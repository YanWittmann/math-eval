package de.yanwittmann.matheval.interpreter.structure;

import de.yanwittmann.matheval.exceptions.MenterExecutionException;
import de.yanwittmann.matheval.lexer.Lexer;
import de.yanwittmann.matheval.lexer.Token;
import de.yanwittmann.matheval.operator.Operator;
import de.yanwittmann.matheval.parser.Parser;
import de.yanwittmann.matheval.parser.ParserNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public abstract class EvaluationContext {

    private static final Logger LOG = LogManager.getLogger(EvaluationContext.class);

    private final EvaluationContext parentContext;
    private Map<String, Value> variables;

    public EvaluationContext(EvaluationContext parentContext) {
        this.parentContext = parentContext;
        this.variables = new HashMap<>();
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

    public Value evaluate(Object node, GlobalContext globalContext, Map<String, Value> localSymbols, SymbolCreationMode symbolCreationMode) {
        Value result = null;

        if (node instanceof ParserNode) {
            result = evaluate((ParserNode) node, globalContext, localSymbols, symbolCreationMode);
        } else if (node instanceof Token) {
            result = evaluate((Token) node, globalContext, localSymbols, symbolCreationMode);
        }

        return result;
    }

    private Value evaluate(ParserNode node, GlobalContext globalContext, Map<String, Value> localSymbols, SymbolCreationMode symbolCreationMode) {
        Value result = null;

        if (node.getType() == ParserNode.NodeType.STATEMENT || node.getType() == ParserNode.NodeType.ROOT || node.getType() == ParserNode.NodeType.CODE_BLOCK) {
            for (Object child : node.getChildren()) {
                result = evaluate(child, globalContext, localSymbols, symbolCreationMode);
            }
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

                LOG.info("Evaluating operator [{}] with arguments {}", op.getSymbol(), Arrays.toString(arguments));

                result = op.evaluate(arguments);
            }
        } else if (node.getType() == ParserNode.NodeType.IDENTIFIER_ACCESSED) {
            result = resolveSymbol(node, symbolCreationMode, globalContext, localSymbols);
            LOG.info("Resolved symbol [{}] to [{}]", node.reconstructCode(), result);

        } else if (node.getType() == ParserNode.NodeType.ASSIGNMENT) {
            final Value variable = evaluate(node.getChildren().get(0), globalContext, localSymbols, SymbolCreationMode.CREATE_IF_NOT_EXISTS);
            final Value value = evaluate(node.getChildren().get(1), globalContext, new HashMap<>(localSymbols), SymbolCreationMode.THROW_IF_NOT_EXISTS);

            LOG.info("Assigning [{}] to variable with previous value [{}]", value, variable);
            variable.inheritValue(value);
            result = value;

        } else if (node.getType() == ParserNode.NodeType.PARENTHESIS_PAIR) {
            result = new Value(node.getChildren().stream()
                    .map(child -> evaluate(child, globalContext, localSymbols, symbolCreationMode))
                    .collect(Collectors.toList()));

        } else if (node.getType() == ParserNode.NodeType.FUNCTION_DECLARATION) {
            final Value functionName = evaluate(node.getChildren().get(0), globalContext, localSymbols, SymbolCreationMode.CREATE_IF_NOT_EXISTS);
            final List<Object> functionArguments = Parser.isType(node.getChildren().get(1), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(1)).getChildren() : null;
            if (functionArguments == null) {
                throw new MenterExecutionException("Function arguments are not a parenthesis pair", node);
            }
            final ParserNode functionCode = (ParserNode) node.getChildren().get(2);

            final Function function = new Function(functionArguments, functionCode);
            functionName.setValue(function);

        } else if (node.getType() == ParserNode.NodeType.FUNCTION_CALL) {
            final Value function = evaluate(node.getChildren().get(0), globalContext, localSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS);

            if (!function.getType().equals(PrimitiveValueType.FUNCTION.getType())) {
                throw new MenterExecutionException("Cannot call non-function value " + function, node);
            }

            final List<Object> functionArguments = Parser.isType(node.getChildren().get(1), ParserNode.NodeType.PARENTHESIS_PAIR) ? ((ParserNode) node.getChildren().get(1)).getChildren() : null;
            if (functionArguments == null) {
                throw new MenterExecutionException("Function arguments are not a parenthesis pair", node);
            }
            final List<Object> evaluatedArguments = functionArguments.stream()
                    .map(argument -> evaluate(argument, globalContext, localSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS))
                    .collect(Collectors.toList());

            final Function executableFunction = (Function) function.getValue();
            final List<String> functionArgumentNames = executableFunction.getArgumentNames();

            if (functionArgumentNames.size() != evaluatedArguments.size()) {
                throw new MenterExecutionException("Function " + function + " requires " + functionArgumentNames.size() + " arguments, but " + evaluatedArguments.size() + " were given", node);
            }

            final Map<String, Value> functionLocalSymbols = new HashMap<>(localSymbols);
            for (int i = 0; i < functionArgumentNames.size(); i++) {
                final String argumentName = functionArgumentNames.get(i);
                final Value argumentValue = (Value) evaluatedArguments.get(i);
                functionLocalSymbols.put(argumentName, argumentValue);
            }

            result = evaluate(executableFunction.getBody(), globalContext, functionLocalSymbols, SymbolCreationMode.THROW_IF_NOT_EXISTS);

            LOG.info("Function [{}] returned [{}]", function, result);
        }

        return result;
    }

    private Value evaluate(Token node, GlobalContext globalContext, Map<String, Value> localSymbols, SymbolCreationMode symbolCreationMode) {
        Value result = null;

        if (node.getType() == Lexer.TokenType.IDENTIFIER) {
            result = resolveSymbol(node, symbolCreationMode, globalContext, localSymbols);
            LOG.info("Resolved symbol [{}] to [{}]", node, result);

        } else if (Parser.isListable(node)) {
            result = new Value(node.getValue());
        }

        return result;
    }

    enum SymbolCreationMode {
        CREATE_IF_NOT_EXISTS,
        THROW_IF_NOT_EXISTS
    }

    private Value resolveSymbol(Object identifier, SymbolCreationMode symbolCreationMode, GlobalContext globalContext, Map<String, Value> localSymbols) {
        // this might be an IDENTIFIER_ACCESSED, IDENTIFIER, a Value, a string

        final List<Object> identifiers = new ArrayList<>();
        if (identifier instanceof ParserNode) {
            final ParserNode node = (ParserNode) identifier;
            if (node.getType() == ParserNode.NodeType.IDENTIFIER_ACCESSED) {
                identifiers.addAll(node.getChildren());
            }
        } else if (identifier instanceof Token) {
            final Token token = (Token) identifier;
            if (token.getType() == Lexer.TokenType.IDENTIFIER) {
                identifiers.add(token);
            }
        } else if (identifier instanceof Value || identifier instanceof String) {
            identifiers.add(identifier);
        } // TODO: handle expressions

        if (identifiers.isEmpty()) {
            throw new MenterExecutionException("Cannot resolve symbol from " + identifier);
        }

        Value value = null;

        for (Object id : identifiers) {
            final String stringKey = ((java.util.function.Function<Object, String>) o -> {
                if (o instanceof Token) {
                    return ((Token) o).getValue();
                } else if (o instanceof Value) {
                    return ((Value) o).getValue().toString();
                } else if (o instanceof String) {
                    return (String) o;
                }
                return null;
            }).apply(id);

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
                    if (anImport.getAliasOrName().equals(stringKey)) {
                        final Module module = anImport.getModule();
                        if (module != null) {
                            globalContext = module.getParentContext();
                            value = null;
                            foundImport = true;
                            break;
                        }
                    }
                }
                if (foundImport) continue;

            } else {
                final Value accessAs = id instanceof Value ? value : new Value(getTokenOrNodeValue(id));
                value = value.access(accessAs);
                if (value != null) {
                    continue;
                } else if (SymbolCreationMode.CREATE_IF_NOT_EXISTS.equals(symbolCreationMode)) {
                    value = previousValue.create(accessAs, new Value(null));
                    continue;
                }
            }

            if (SymbolCreationMode.THROW_IF_NOT_EXISTS == symbolCreationMode) {
                throw new MenterExecutionException("Cannot resolve symbol '" + stringKey + "' on " + identifier);
            } else if (SymbolCreationMode.CREATE_IF_NOT_EXISTS == symbolCreationMode) {
                if (isFinalIdentifier) {
                    value = new Value(null);
                } else {
                    value = new Value(new HashMap<>());
                }
                globalContext.addVariable(stringKey, value);
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
