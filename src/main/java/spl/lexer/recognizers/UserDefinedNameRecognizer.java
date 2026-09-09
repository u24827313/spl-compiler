package spl.lexer.recognizers;

import spl.lexer.Token;
import spl.lexer.TokenType;

/**
 * Owner: TODO(assign teammate)
 *
 * Regex from spec: #(0|1|...|9|a|b|...|z)*_
 * Designed so it can never collide with a reserved keyword (all keywords
 * are plain lowercase words with no leading '#').
 */
public class UserDefinedNameRecognizer implements TokenRecognizer {
    @Override
    public Token tryMatch(String source, int pos, int line, int column) {
        // TODO: implement USER-DEFINED-NAME regex matching per spec.
        return null;
    }
}
