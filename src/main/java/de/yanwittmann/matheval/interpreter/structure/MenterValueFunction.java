package de.yanwittmann.matheval.interpreter.structure;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface MenterValueFunction {
    Value apply(GlobalContext context, Value self, List<Value> parameters, Map<String, Value> localSymbols);
}
