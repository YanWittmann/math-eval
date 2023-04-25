package de.yanwittmann.menter.interpreter.structure.value;

import de.yanwittmann.menter.interpreter.structure.EvaluationContextLocalInformation;
import de.yanwittmann.menter.interpreter.structure.GlobalContext;

import java.util.List;

public interface NativeFunction {
    Value execute(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> parameters);
}
