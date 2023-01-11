package de.yanwittmann.menter.interpreter.structure;

import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.util.List;

@FunctionalInterface
public interface MenterValueFunction {
    Value apply(GlobalContext context, Value self, List<Value> parameters, EvaluationContextLocalInformation localInformation);
}
