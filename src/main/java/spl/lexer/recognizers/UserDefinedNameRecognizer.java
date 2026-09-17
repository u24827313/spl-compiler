package spl.lexer.recognizers;

import spl.lexer.Token;
import spl.lexer.TokenType;

/**
 * Owner: Donovan
 *
 * Regex from spec: #(0|1|...|9|a|b|...|z)*_
 * Designed so it can never collide with a reserved keyword (all keywords
 * are plain lowercase words with no leading '#').
 */
public class UserDefinedNameRecognizer implements TokenRecognizer {
    @Override
    public Token tryMatch(String source, int pos, int line, int column) {
        if (pos < 0 || pos >= source.length()) {
            return null;
        }

        if (source.charAt(pos) != '#') {
            return null;
        }

        for (int i = pos + 1; i < source.length(); i++) {
            char ch = source.charAt(i);

            if (isBlankSpace(ch)) {
                String lexeme = source.substring(pos, i);
                return new Token(TokenType.USER_DEFINED_NAME, lexeme, line, column);
            }

            if (!(isLetter(ch) || isDigit(ch))) {
                return null;
            }
        }

        return null;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isLetter(char c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean isBlankSpace(char c) {
        // Per spec: ASCII 32 (space) or ASCII 13 (CR) - NOT ASCII 10 (LF).
        return c == 32 || c == 13;
    }
}
