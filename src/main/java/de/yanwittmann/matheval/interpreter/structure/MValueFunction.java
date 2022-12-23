package de.yanwittmann.matheval.interpreter.structure;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface MValueFunction {
    Value apply(GlobalContext context, Value self, List<Value> parameters, Map<String, Value> localSymbols);
}
