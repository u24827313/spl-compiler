package spl.lexer;

import org.junit.jupiter.api.Test;
import spl.lexer.recognizers.UserDefinedNameRecognizer;

import static org.junit.jupiter.api.Assertions.*;

class UserDefinedNameRecognizerTest {

    private final UserDefinedNameRecognizer recognizer = new UserDefinedNameRecognizer();

    @Test
    void matchesSimpleName() {
        Token t = recognizer.tryMatch("#x1 ", 0, 1, 1);
        assertNotNull(t, "TODO: implement UserDefinedNameRecognizer");
        assertEquals(TokenType.USER_DEFINED_NAME, t.type());
        assertEquals("#x1", t.lexeme());
    }

    @Test
    void rejectsMissingHash() {
        Token t = recognizer.tryMatch("x1 ", 0, 1, 1);
        assertNull(t);
    }
}
