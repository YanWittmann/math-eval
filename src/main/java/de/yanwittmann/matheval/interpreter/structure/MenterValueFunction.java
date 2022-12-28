package de.yanwittmann.matheval.interpreter.structure;

import java.util.List;

@FunctionalInterface
public interface MenterValueFunction {
    Value apply(GlobalContext context, Value self, List<Value> parameters, EvaluationContextLocalInformation localInformation);
}
