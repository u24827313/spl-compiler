package spl.parser;

/**
 * Thrown on a syntax error. Per the spec, the message must be meaningful and
 * include hints the user can understand (e.g. "expected ';' after INSTR at
 * line 12, but found 'print'").
 */
public class ParseException extends RuntimeException {
    public ParseException(String message) {
        super(message);
    }
}
