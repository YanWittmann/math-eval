package de.yanwittmann.matheval.exceptions;

public class ContextInitializationException extends RuntimeException {

        public ContextInitializationException(String message) {
            super(message);
        }

        public ContextInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
}
