package de.yanwittmann.matheval.exceptions;

import de.yanwittmann.matheval.interpreter.structure.EvaluationContext;
import de.yanwittmann.matheval.interpreter.structure.GlobalContext;
import de.yanwittmann.matheval.parser.ParserNode;

public class MenterExecutionException extends RuntimeException {

    public MenterExecutionException(String message) {
        super(message);
    }

    public MenterExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MenterExecutionException(String message, ParserNode node) {
        super(message + "\n" + node.reconstructCode());
    }

    public MenterExecutionException(EvaluationContext context, String message, ParserNode node) {
        super((context instanceof GlobalContext ? ((GlobalContext) context).getSourceName() + ": " : "") + message + "\n" + node.reconstructCode());
    }

    public MenterExecutionException(EvaluationContext context, String message) {
        super((context instanceof GlobalContext ? ((GlobalContext) context).getSourceName() + ": " : "") + message);
    }
}
