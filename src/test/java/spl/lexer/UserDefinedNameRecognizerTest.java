package spl.lexer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import spl.lexer.recognizers.UserDefinedNameRecognizer;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserDefinedNameRecognizerTest {

    private final UserDefinedNameRecognizer recognizer = new UserDefinedNameRecognizer();

    @ParameterizedTest(name = "valid: {0}")
    @MethodSource("validNames")
    void validNames(String source, String expectedLexeme) {
        Token token = recognizer.tryMatch(source, 0, 1, 1);

        assertToken(token, expectedLexeme, 1, 1);
    }

    static Stream<Arguments> validNames() {
        return Stream.of(
                Arguments.of("# ", "#"),
                                Arguments.of("#\r", "#"),
                Arguments.of("#a ", "#a"),
                Arguments.of("#z ", "#z"),
                Arguments.of("#0 ", "#0"),
                Arguments.of("#9 ", "#9"),
                Arguments.of("#abc ", "#abc"),
                Arguments.of("#0123456789 ", "#0123456789"),
                Arguments.of("#a0b1c2z9 ", "#a0b1c2z9"),
                Arguments.of("#000abc999xyz ", "#000abc999xyz"),
                Arguments.of("#abcdefghijklmnopqrstuvwxyz0123456789 ",
                        "#abcdefghijklmnopqrstuvwxyz0123456789"),
                Arguments.of("#abc  ", "#abc")
        );
    }

    @ParameterizedTest(name = "allowed character: {0}")
    @MethodSource("allowedSingleCharacters")
    void allowedCharacters(char allowedCharacter) {
        String source = "#" + allowedCharacter + " ";

        Token token = recognizer.tryMatch(source, 0, 1, 1);

        assertToken(token, "#" + allowedCharacter, 1, 1);
    }

    static Stream<Character> allowedSingleCharacters() {
        Stream<Character> digits = IntStream.rangeClosed('0', '9')
                .mapToObj(value -> (char) value);
        Stream<Character> lowercaseLetters = IntStream.rangeClosed('a', 'z')
                .mapToObj(value -> (char) value);
        return Stream.concat(digits, lowercaseLetters);
    }

    @Test
    void carriageReturnDelimiter() {
        Token token = recognizer.tryMatch("#name7\r", 0, 3, 8);

        assertToken(token, "#name7", 3, 8);
    }

    @Test
    void spaceTerminatesName() {
        Token token = recognizer.tryMatch("#first #second ", 0, 1, 1);

        assertToken(token, "#first", 1, 1);
    }

    @Test
    void ignoresTextAfterSpace() {
        Token token = recognizer.tryMatch("#abc !invalid", 0, 1, 1);

        assertToken(token, "#abc", 1, 1);
    }

    @Test
    void carriageReturnTerminatesName() {
        Token token = recognizer.tryMatch("#abc\r#def ", 0, 1, 1);

        assertToken(token, "#abc", 1, 1);
    }

    @Test
    void longName() {
        String lexeme = "#" + "abcdefghijklmnopqrstuvwxyz0123456789".repeat(1_000);

        Token token = recognizer.tryMatch(lexeme + " ", 0, 1, 1);

        assertToken(token, lexeme, 1, 1);
    }

    @Test
    void callsAreIndependent() {
        assertToken(recognizer.tryMatch("#first1 ", 0, 2, 3), "#first1", 2, 3);
        assertNull(recognizer.tryMatch("#invalid! ", 0, 4, 5));
        assertToken(recognizer.tryMatch("# ", 0, 6, 7), "#", 6, 7);
    }

    @Test
    void nonZeroOffset() {
        Token token = recognizer.tryMatch("ignored#value42 rest", 7, 6, 12);

        assertToken(token, "#value42", 6, 12);
    }

    @Test
    void noLookAhead() {
        Token token = recognizer.tryMatch("ignored#value42 ", 0, 1, 1);

        assertNull(token);
    }

    @ParameterizedTest(name = "invalid: {0}")
    @ValueSource(strings = {
            "",
            " ",
            "\r",
            " #abc ",
            "#",
            "name ",
            "name# ",
            "## ",
            "#abc#def ",
            "#UPPER ",
            "#camelCase ",
            "#abc_ ",
            "#abc- ",
            "#abc. ",
            "#abc/def ",
            "#abc! ",
            "#abc? ",
            "#abc@ ",
            "#abc\t ",
            "#abc\n ",
            "#é ",
            "#α ",
            "#\u0661 ",
            "#\uFF11 ",
            "#🙂 "
    })
    void invalidNames(String source) {
        Token token = recognizer.tryMatch(source, 0, 1, 1);

        assertNull(token);
    }

    @ParameterizedTest(name = "missing delimiter: {0}")
    @ValueSource(strings = {
            "#a",
            "#0",
            "#abc123",
            "#abcdefghijklmnopqrstuvwxyz0123456789"
    })
    void missingDelimiter(String source) {
        Token token = recognizer.tryMatch(source, 0, 1, 1);

        assertNull(token);
    }

    @ParameterizedTest(name = "invalid delimiter: {0}")
    @ValueSource(strings = {
            "#abc\t",
            "#abc\n",
            "#abc\f",
            "#abc\u00A0"
    })
    void invalidDelimiters(String source) {
        Token token = recognizer.tryMatch(source, 0, 1, 1);

        assertNull(token);
    }

    @Test
    void hashAtEnd() {
        assertNull(recognizer.tryMatch("abc#", 3, 1, 4));
    }

    @Test
    void negativePosition() {
        assertNull(recognizer.tryMatch("#abc ", -1, 1, 1));
    }

    @Test
    void positionAtEnd() {
        String source = "#abc ";

        assertNull(recognizer.tryMatch(source, source.length(), 1, 1));
    }

    @Test
    void positionPastEnd() {
        String source = "#abc ";

        assertNull(recognizer.tryMatch(source, source.length() + 1, 1, 1));
    }

    private static void assertToken(Token token, String expectedLexeme,
                                    int expectedLine, int expectedColumn) {
        assertNotNull(token);
        assertAll(
                () -> assertEquals(TokenType.USER_DEFINED_NAME, token.type()),
                () -> assertEquals(expectedLexeme, token.lexeme()),
                () -> assertEquals(expectedLine, token.line()),
                () -> assertEquals(expectedColumn, token.column())
        );
    }
}
