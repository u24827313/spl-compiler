package spl.lexer;

import spl.lexer.recognizers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges the independently-developed category recognizers into one token
 * stream. Whoever integrates this should NOT need to touch individual
 * recognizer implementations — only this orchestration logic.
 *
 * Order matters where prefixes could overlap; KeywordRecognizer should run
 * before others only if that ever becomes ambiguous (per the spec, it isn't,
 * since USER-DEFINED-NAME always starts with '#').
 */
public class Lexer {

    private final List<TokenRecognizer> recognizers = List.of(
            new KeywordRecognizer(),
            new UserDefinedNameRecognizer(),
            new NumRecognizer(),
            new StringRecognizer()
    );

    public List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0, line = 1, column = 1;

        while (pos < source.length()) {
            // TODO: skip leading blank_space (ASCII 32 / 13) between tokens,
            // updating line/column as you go.

            Token match = null;
            for (TokenRecognizer r : recognizers) {
                match = r.tryMatch(source, pos, line, column);
                if (match != null) break;
            }

            if (match == null) {
                throw new LexException("Unrecognized token at line " + line + ", column " + column);
            }

            tokens.add(match);
            pos += match.lexeme().length(); // TODO: adjust once blank_space handling is final
        }

        tokens.add(new Token(TokenType.EOF, "$", line, column));
        return tokens;
    }

    public static class LexException extends RuntimeException {
        public LexException(String message) { super(message); }
    }
}
