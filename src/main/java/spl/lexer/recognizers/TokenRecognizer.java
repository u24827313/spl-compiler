package spl.lexer.recognizers;

import spl.lexer.Token;

/**
 * Contract for a single lexical-category recognizer (NUM, USER-DEFINED-NAME,
 * STRING, keywords/symbols, ...). Each team member owning a category
 * implements this against ONLY their slice of the source text, so recognizers
 * can be unit-tested and merged independently.
 *
 * Convention: every token must be immediately followed by a blank_space
 * (ASCII 32 or 13) per the spec — recognizers should consume up to and
 * including that trailing blank_space, or fail cleanly if the pattern
 * doesn't match at the given position.
 */
public interface TokenRecognizer {

    /**
     * Attempt to recognize this category's token starting at `pos` in `source`.
     *
     * @return the matched Token, or null if this recognizer does not match here
     *         (the Lexer will then try the next recognizer in its chain).
     */
    Token tryMatch(String source, int pos, int line, int column);
}
