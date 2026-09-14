package spl.lexer.recognizers;

import spl.lexer.Token;
import spl.lexer.TokenType;

/**
 * Owner: Ayrtonn Taljaard (u24856462)
 *
 * Regex from spec: "(,|.|:|–|?|!|0|...|9|a|...|z)*"_
 * Note the opening/closing double-quote are part of the token itself.
 */
public class StringRecognizer implements TokenRecognizer 
{
    @Override
    public Token tryMatch(String source, int pos, int line, int column) 
    {
        // Strings require a Quotation Mark to begin the String
        if (pos >= source.length() || source.charAt(pos) != '"') 
        {
            return null;
        }

        // Skip the quotation mark
        int i = pos + 1;

        // Walk through String
        while (i < source.length()) 
        {
            char c = source.charAt(i);

            // 2nd Quotation Mark indicates end of String
            if (c == '"') 
            {
                int closingQuote = i;

                // Move one position past the String
                i++;

                // Checking that there is a valid whitespace after the String
                if (i < source.length() && isBlankSpace(source.charAt(i))) 
                {
                    // Extracting entire String (excluding the whitespace delimiter)
                    String lexeme = source.substring(pos, closingQuote + 1);
                    return new Token(
                        TokenType.STRING,
                        lexeme,
                        line,
                        column
                    );
                }

                return null;
            }

            // Checking char is within regex's scope
            if (!isAllowedStringChar(c)) 
            {
                return null;
            }

            i++;
        }

        return null;
    }

    // Used to check char is included in the regex
    private boolean isAllowedStringChar(char c) 
    {
        return (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == ','
                || c == '.'
                || c == ':'
                || c == '–'
                || c == '?'
                || c == '!';
    }

    // Allows ASCII values: 32, 13 and 19 to indiciate a valid blank space
    private boolean isBlankSpace(char c) 
    {
        return c == ' ' || c == '\r' || c == '\n';
    }
}
