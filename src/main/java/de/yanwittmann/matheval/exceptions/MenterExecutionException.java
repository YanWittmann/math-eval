package de.yanwittmann.matheval.exceptions;

public class MenterExecutionException extends RuntimeException {

    final boolean containsStackTrace;

    public MenterExecutionException(String message) {
        super(message);
        containsStackTrace = hasStackTrace(message);
    }

    public MenterExecutionException(String message, Throwable cause) {
        super(message, cause);
        containsStackTrace = hasStackTrace(message);
    }

    public boolean hasStackTrace() {
        return containsStackTrace;
    }

    private static boolean hasStackTrace(String message) {
        return message.contains("Local symbols");
    }
}
