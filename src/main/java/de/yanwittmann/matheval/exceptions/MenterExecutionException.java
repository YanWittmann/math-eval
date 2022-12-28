package de.yanwittmann.matheval.exceptions;

public class MenterExecutionException extends RuntimeException {

    public MenterExecutionException(String message) {
        super(message);
    }

    public MenterExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
