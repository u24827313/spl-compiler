package spl.parser;

import spl.lexer.Token;

/** Thrown when the parser encounters source that does not match SPL syntax. */
public class ParseException extends RuntimeException {
    private final SyntaxErrorType errorType;
    private final Token offendingToken;

    public ParseException(SyntaxErrorType errorType, String message, Token offendingToken) {
        super(message);
        this.errorType = errorType;
        this.offendingToken = offendingToken;
    }

    public String getDescription() {
        return errorType.description();
    }

    public int getLine() {
        return offendingToken.line();
    }

    public int getColumn() {
        return offendingToken.column();
    }

    public String getOffendingText() {
        return offendingToken.lexeme();
    }
}
