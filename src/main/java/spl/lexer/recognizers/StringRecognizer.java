package spl.lexer.recognizers;

import spl.lexer.Token;
import spl.lexer.TokenType;

/**
 * Owner: TODO(assign teammate)
 *
 * Regex from spec: "(,|.|:|–|?|!|0|...|9|a|...|z)*"_
 * Note the opening/closing double-quote are part of the token itself.
 */
public class StringRecognizer implements TokenRecognizer {
    @Override
    public Token tryMatch(String source, int pos, int line, int column) {
        // TODO: implement STRING regex matching per spec.
        return null;
    }
}
