package spl.lexer;

/**
 * A single lexed token. `lexeme` is the exact source text (used later as the
 * "contents" field of a leaf node when writing tree.xml).
 */
public record Token(TokenType type, String lexeme, int line, int column) {
    @Override
    public String toString() {
        return type + "('" + lexeme + "')";
    }
}
