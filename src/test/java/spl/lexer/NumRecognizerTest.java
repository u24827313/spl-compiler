package spl.lexer;

import org.junit.jupiter.api.Test;
import spl.lexer.recognizers.NumRecognizer;

import static org.junit.jupiter.api.Assertions.*;

class NumRecognizerTest {

    private final NumRecognizer recognizer = new NumRecognizer();

    @Test
    void matchesSimpleZero() {
        // "0 " -> single 0 token per "0_" alternative
        Token t = recognizer.tryMatch("0 ", 0, 1, 1);
        assertNotNull(t, "TODO: implement NumRecognizer");
        assertEquals(TokenType.NUM, t.type());
        assertEquals("0", t.lexeme());
    }

    @Test
    void matchesNegativeDecimal() {
        Token t = recognizer.tryMatch("-3.14 ", 0, 1, 1);
        assertNotNull(t);
        assertEquals("-3.14", t.lexeme());
    }

    @Test
    void rejectsLeadingZeroInteger() {
        // "01 " should NOT match any NUM alternative (no leading-zero integers except "0" itself)
        Token t = recognizer.tryMatch("01 ", 0, 1, 1);
        assertNull(t);
    }
}
