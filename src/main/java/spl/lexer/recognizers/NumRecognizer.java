package spl.lexer.recognizers;

import spl.lexer.Token;
import spl.lexer.TokenType;

/**
 * Owner: TODO(assign teammate)
 *
 * Regex from spec:
 *   0_ |
 *   (–|ε)0.(0|1|...|9)*(1|...|9)_ |
 *   (–|ε)(1|...|9)(0|...|9)*.(0|...|9)*(1|...|9)_ |
 *   (–|ε)(1|...|9)(0|...|9)*_
 * (where _ denotes the mandatory trailing blank_space)
 */
public class NumRecognizer implements TokenRecognizer {
    @Override
    public Token tryMatch(String source, int pos, int line, int column) {
        // TODO: implement NUM regex matching per spec.
        return null;
    }
}
