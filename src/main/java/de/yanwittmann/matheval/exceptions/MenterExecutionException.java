package de.yanwittmann.matheval.exceptions;

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
}
