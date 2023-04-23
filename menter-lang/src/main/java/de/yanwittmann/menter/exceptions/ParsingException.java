package de.yanwittmann.menter.exceptions;

import de.yanwittmann.menter.parser.ParserNode;

import java.util.ArrayList;
import java.util.Arrays;
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

    private final static int ADDITIONAL_MESSAGE_PADDING = 10;
    private final static String MESSAGE_LEFT_DELIMITER = "-->>";
    private final static String MESSAGE_RIGHT_DELIMITER = "<<--";

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
                    sb.add(MESSAGE_LEFT_DELIMITER);
                }
                sb.add(ParserNode.reconstructCode(tokens.get(i)));
                if (i == index) {
                    sb.add(MESSAGE_RIGHT_DELIMITER);
                }
            }

            if (index + ADDITIONAL_MESSAGE_PADDING < tokens.size()) {
                sb.add("...");
            }

            // remove all from left and right until the string is short enough
            final String joined = sb.toString();
            final List<String> split = Arrays.asList(joined.split(" "));

            final int includeLeftBorder = split.indexOf(MESSAGE_LEFT_DELIMITER);
            final int includeRightBorder = split.lastIndexOf(MESSAGE_RIGHT_DELIMITER);

            // add center part
            final List<String> selection = new ArrayList<>(
                    split.subList(includeLeftBorder, includeRightBorder + 1)
            );
            final int centerSize = selection.size();

            int caretLeft = includeLeftBorder - 1;
            int caretRight = includeRightBorder + 1;

            while (true) {
                if ((selection.size() - centerSize) > ADDITIONAL_MESSAGE_PADDING) break;

                if (caretLeft >= 0) {
                    selection.add(0, split.get(caretLeft));
                    caretLeft--;
                }
                if (caretRight < split.size()) {
                    selection.add(split.get(caretRight));
                    caretRight++;
                }

                if (caretLeft < 0 && caretRight >= split.size()) break;
            }

            additionalMessage = String.join(" ", selection);
        }

        return additionalMessage;
    }
}
