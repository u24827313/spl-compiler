package spl.lexer;

import org.junit.jupiter.api.Test;
import spl.lexer.recognizers.StringRecognizer;

import static org.junit.jupiter.api.Assertions.*;

class StringRecognizerTest {

    private final StringRecognizer recognizer = new StringRecognizer();

    @Test
    void matchesSimpleString() {
        Token t = recognizer.tryMatch("\"hello\" ", 0, 1, 1);
        assertNotNull(t, "TODO: implement StringRecognizer");
        assertEquals(TokenType.STRING, t.type());
        assertEquals("\"hello\"", t.lexeme());
    }
}
