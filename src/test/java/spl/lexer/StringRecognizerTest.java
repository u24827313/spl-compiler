package spl.lexer;

import org.junit.jupiter.api.Test;
import spl.lexer.recognizers.StringRecognizer;

import static org.junit.jupiter.api.Assertions.*;

class StringRecognizerTest 
{

    private final StringRecognizer recognizer = new StringRecognizer();

    @Test
    void acceptsValidString() 
    {
        Token token = recognizer.tryMatch("\"hello\" ", 0, 1, 1);

        assertNotNull(token);
        assertEquals(TokenType.STRING, token.type());
        assertEquals("\"hello\"", token.lexeme());
        assertEquals(1, token.line());
        assertEquals(1, token.column());
    }

    @Test
    void acceptsEmptyString() 
    {
        Token token = recognizer.tryMatch("\"\" ", 0, 1, 1);

        assertNotNull(token);
        assertEquals(TokenType.STRING, token.type());
        assertEquals("\"\"", token.lexeme());
    }

    @Test
    void acceptsDigitsAndPunctuation() 
    {
        Token token = recognizer.tryMatch("\"abc123!?.,:\" ", 0, 1, 1);

        assertNotNull(token);
        assertEquals("\"abc123!?.,:\"", token.lexeme());
    }

    @Test
    void rejectsUppercaseLetters() 
    {
        assertNull(recognizer.tryMatch("\"Hello\" ", 0, 1, 1));
    }

    @Test
    void rejectsSpaceInsideString() 
    {
        assertNull(recognizer.tryMatch("\"hello world\" ", 0, 1, 1));
    }

    @Test
    void rejectsMissingClosingQuote() 
    {
        assertNull(recognizer.tryMatch("\"hello ", 0, 1, 1));
    }

    @Test
    void rejectsMissingTrailingWhitespace() 
    {
        assertNull(recognizer.tryMatch("\"hello\"", 0, 1, 1));
    }

    @Test
    void acceptsFromCorrectPosition() 
    {
        String source = "abc \"hello\" ";

        Token token = recognizer.tryMatch(source, 4, 1, 5);

        assertNotNull(token);
        assertEquals("\"hello\"", token.lexeme());
        assertEquals(1, token.line());
        assertEquals(5, token.column());
    }
}