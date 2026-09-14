package spl.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import spl.lexer.recognizers.KeywordRecognizer;

public class KeywordRecognizerTest {
    private final KeywordRecognizer recognizer = new KeywordRecognizer();

    @Test
    void matchedSimpleKeywordFollowedBySpace() {
        Token token = recognizer.tryMatch("if ", 0, 1, 1);
        assertNotNull(token);
        assertEquals(TokenType.IF, token.type());
        assertEquals("if", token.lexeme());
    }

    @Test
    void matchedKeywordFollowedByCarriageReturn() {
        Token token = recognizer.tryMatch("while\r", 0, 1, 1);
        assertNotNull(token);
        assertEquals(TokenType.WHILE, token.type());
        assertEquals("while", token.lexeme());
    }

    @Test
    void matchedAllExpectedKeyWords() {
        String[][] cases = {
                {"void ", "VOID"}, {"num ", "NUM_KW"}, {"return ", "RETURN"},
                {"print ", "PRINT"}, {"nop ", "NOP"}, {"comment ", "COMMENT"},
                {"mod ", "MOD"}, {"add ", "ADD"}, {"sub ", "SUB"},
                {"mul ", "MUL"}, {"div ", "DIV"}, {"neg ", "NEG"},
                {"if ", "IF"}, {"then ", "THEN"}, {"else ", "ELSE"},
                {"not ", "NOT"}, {"and ", "AND"}, {"or ", "OR"},
                {"eq ", "EQ"}, {"larger ", "LARGER"}, {"lesser ", "LESSER"},
                {"do ", "DO"}, {"while ", "WHILE"}, {"until ", "UNTIL"}
        };
        for (String[] c : cases) {
            Token token = recognizer.tryMatch(c[0], 0, 1, 1);
            assertNotNull(token, "Expected a match for: " + c[0]);
            assertEquals(c[1], token.type().name(), "Wrong TokenType for: " + c[0]);
        }
    }

    @Test
    void matchedKeywordInMiddleOfSource() {
        Token token = recognizer.tryMatch("x do while", 2, 1, 3);
        assertNotNull(token);
        assertEquals(TokenType.DO, token.type());
        assertEquals("do", token.lexeme());
    }

    @Test
    void matchedPunctuationFollowedBySpace() {
        Token token = recognizer.tryMatch("( ", 0, 1, 1);
        assertNotNull(token);
        assertEquals(TokenType.LPAREN, token.type());
        assertEquals("(", token.lexeme());
    }

    @Test
    void matchedAllExpectedPunctuation() {
        String[][] cases = {
                {"$ ", "EOF"}, {": ", "COLON"}, {"; ", "SEMICOLON"},
                {"( ", "LPAREN"}, {") ", "RPAREN"}, {"{ ", "LBRACE"},
                {"} ", "RBRACE"}, {"= ", "EQUALS"}
        };
        for (String[] c : cases) {
            Token token = recognizer.tryMatch(c[0], 0, 1, 1);
            assertNotNull(token, "Expected a match for: " + c[0]);
            assertEquals(c[1], token.type().name(), "Wrong TokenType for: " + c[0]);
        }
    }

    @Test
    void rejectsKeyworkdNOtFollowedByBlankSpace() {
        assertNull(recognizer.tryMatch("if(x)", 0, 1, 1));
    }

    @Test
    void rejectsPunctuationNotFollowedByBlankSpace() {
        assertNull(recognizer.tryMatch("(x", 0, 1, 1));
    }

    @Test
    void rejectsKeywordAtEndOfSourceWithNoTrailingSpace() {
        assertNull(recognizer.tryMatch("do", 0, 1, 1));
    }

    @Test
    void rejectsUnknownWord() {
        assertNull(recognizer.tryMatch("foobar ", 0, 1, 1));
    }

    @Test
    void rejectsWordThatIsPrefixOfKeyword() {
        assertNull(recognizer.tryMatch("i ", 0, 1, 1));
    }

    @Test
    void rejectsWordThatHasKeywordAsPrefixButIsLonger() {
        assertNull(recognizer.tryMatch("iffy ", 0, 1, 1));
    }

    @Test
    void rejectsUserDefinedNameStart() {
        assertNull(recognizer.tryMatch("#name ", 0, 1, 1));
    }

    @Test
    void rejectsDigitStart() {
        assertNull(recognizer.tryMatch("123 ", 0, 1, 1));
    }

    @Test
    void rejectsStringStart() {
        assertNull(recognizer.tryMatch("\"hello\" ", 0, 1, 1));
    }

    @Test
    void rejectsEmptySource() {
        assertNull(recognizer.tryMatch("", 0, 1, 1));
    }

    @Test
    void rejectsPositionAtEndOfSource() {
        assertNull(recognizer.tryMatch("do ", 3, 1, 4));
    }

    @Test
    void preservesLineAndColumnInToken() {
        Token token = recognizer.tryMatch("nop ", 0, 5, 10);
        assertNotNull(token);
        assertEquals(5, token.line());
        assertEquals(10, token.column());
    }
}
