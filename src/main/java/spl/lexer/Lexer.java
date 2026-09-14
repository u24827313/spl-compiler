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
public class Lexer 
{

    private final List<TokenRecognizer> recognizers = List.of(
            new KeywordRecognizer(),
            new UserDefinedNameRecognizer(),
            new NumRecognizer(),
            new StringRecognizer()
    );

    public List<Token> tokenize(String source) 
    {
        // Initialize start positions and empty tokens list
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int line = 1; 
        int column = 1;

        // Loop until end of source is reached
        while (pos < source.length()) 
        {
            char current = source.charAt(pos);

            // If a whitespace is encountered
            if(current == ' ' || current == '\r' || current == '\n') 
            {

                // If whitespace represents a new line
                if (current == '\n' || current == '\r') 
                {
                    line++;
                    column = 1;
                }
                else
                {
                    column++;
                }
                
                // Move past whitespace
                pos++;
                continue;
            }

            Token matchedToken = null;

            // Iterate through Recognizer DFAs to see if any can find a match
            for (TokenRecognizer recognizer : recognizers) 
            {
                matchedToken = recognizer.tryMatch(source, pos, line, column);

                // If a DFA found a match break out the loop
                if (matchedToken != null) break;
            }

            // If no match, it cannot be processed
            if (matchedToken == null) 
            {
                throw new LexException("Unrecognized token at line " + line + ", column " + column);
            }

            tokens.add(matchedToken);

            // Move position and column to the start of next token
            int tokenLength = matchedToken.lexeme().length();
            pos += tokenLength;
            column += tokenLength;
        }

        tokens.add(new Token(TokenType.EOF, "$", line, column));
        return tokens;
    }

    public static class LexException extends RuntimeException 
    {
        public LexException(String message) { super(message); }
    }
}
