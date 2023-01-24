package de.yanwittmann.menter.exceptions;

import de.yanwittmann.menter.parser.ParserNode;

import java.util.List;
import java.util.StringJoiner;

public class ParsingException extends RuntimeException {

    public ParsingException(String message) {
        super(message);
    }

    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParsingException(String message, Object token, List<Object> tokens) {
        this(message + "\n" + createAdditionalMessage(token, tokens));
    }

    private final static int ADDITIONAL_MESSAGE_PADDING = 7;

    private static String createAdditionalMessage(Object token, List<Object> tokens) {
        int index = tokens.indexOf(token);
        final String additionalMessage;

        if (index == -1) {
            additionalMessage = ParserNode.reconstructCode(token);
        } else {
            final StringJoiner sb = new StringJoiner(" ");

            if (index - ADDITIONAL_MESSAGE_PADDING > 0) {
                sb.add("...");
            }

            for (int i = Math.max(0, index - ADDITIONAL_MESSAGE_PADDING); i < Math.min(tokens.size(), index + ADDITIONAL_MESSAGE_PADDING); i++) {
                if (i == index) {
                    sb.add(">>>");
                }
                sb.add(ParserNode.reconstructCode(tokens.get(i)));
                if (i == index) {
                    sb.add("<<<");
                }
            }

            if (index + ADDITIONAL_MESSAGE_PADDING < tokens.size()) {
                sb.add("...");
            }

            additionalMessage = sb.toString();
        }

        return additionalMessage;
    }
}
