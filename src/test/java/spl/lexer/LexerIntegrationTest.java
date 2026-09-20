package spl.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerIntegrationTest {

    private final Lexer lexer = new Lexer();

    @Test
    void tokenizesSimpleVarDeclarationAndAssignment() {
        String source = "#x #y : : #x = 5 ; $";

        List<Token> tokens = lexer.tokenize(source);

        assertEquals(TokenType.USER_DEFINED_NAME, tokens.get(0).type());
        assertEquals(TokenType.USER_DEFINED_NAME, tokens.get(1).type());
        assertEquals(TokenType.COLON, tokens.get(2).type());
        assertEquals(TokenType.COLON, tokens.get(3).type());
        assertEquals(TokenType.USER_DEFINED_NAME, tokens.get(4).type());
        assertEquals(TokenType.EQUALS, tokens.get(5).type());
        assertEquals(TokenType.NUM, tokens.get(6).type());
        assertEquals(TokenType.SEMICOLON, tokens.get(7).type());
        assertEquals(TokenType.EOF, tokens.get(8).type());
    }

    @Test
    void tokenizesKeywordsAndStringsTogether() {
        String source = "print \"hello\" ; comment \"note\" ; $";

        List<Token> tokens = lexer.tokenize(source);

        assertEquals(TokenType.PRINT, tokens.get(0).type());
        assertEquals(TokenType.STRING, tokens.get(1).type());
        assertEquals(TokenType.SEMICOLON, tokens.get(2).type());
        assertEquals(TokenType.COMMENT, tokens.get(3).type());
        assertEquals(TokenType.STRING, tokens.get(4).type());
        assertEquals(TokenType.SEMICOLON, tokens.get(5).type());
        assertEquals(TokenType.EOF, tokens.get(6).type());
    }

    @Test
    void tokenizesFunctionDeclarationSkeleton() {
        // void #f ( ) { return } -- exercises keyword + punctuation + name together
        String source = "void #f ( ) { return } $";

        List<Token> tokens = lexer.tokenize(source);

        assertEquals(TokenType.VOID, tokens.get(0).type());
        assertEquals(TokenType.USER_DEFINED_NAME, tokens.get(1).type());
        assertEquals(TokenType.LPAREN, tokens.get(2).type());
        assertEquals(TokenType.RPAREN, tokens.get(3).type());
        assertEquals(TokenType.LBRACE, tokens.get(4).type());
        assertEquals(TokenType.RETURN, tokens.get(5).type());
        assertEquals(TokenType.RBRACE, tokens.get(6).type());
        assertEquals(TokenType.EOF, tokens.get(7).type());
    }

    @Test
    void handlesCarriageReturnAsSeparator() {
        String source = "#x\r#y\r\n$";

        List<Token> tokens = lexer.tokenize(source);

        assertEquals(TokenType.USER_DEFINED_NAME, tokens.get(0).type());
        assertEquals(TokenType.USER_DEFINED_NAME, tokens.get(1).type());
        assertEquals(TokenType.EOF, tokens.get(2).type());
    }

    @Test
    void throwsOnUnrecognizedCharacter() {
        // '@' isn't valid anywhere in the SPL lexical grammar
        String source = "@ $";

        Lexer.LexException ex = assertThrows(Lexer.LexException.class,
                () -> lexer.tokenize(source));
        assertTrue(ex.getMessage().contains("line"));
    }

    @Test
    void tokenizesEmptyProgramSkeleton() {
        // V_DECL=ε : F_DECL=ε : ALGO=ε $
        String source = ": : $";

        List<Token> tokens = lexer.tokenize(source);

        assertEquals(TokenType.COLON, tokens.get(0).type());
        assertEquals(TokenType.COLON, tokens.get(1).type());
        assertEquals(TokenType.EOF, tokens.get(2).type());
    }

    @Test
    void handlesMultipleSpacesBetweenTokens() {
        String source = "#x    #y $";  // several spaces between tokens
        List<Token> tokens = lexer.tokenize(source);
        assertEquals(TokenType.USER_DEFINED_NAME, tokens.get(0).type());
        assertEquals(TokenType.USER_DEFINED_NAME, tokens.get(1).type());
    }

    @Test
    void rejectsNegativeBareZero() {
        String source = "-0 $";
        assertThrows(Lexer.LexException.class, () -> lexer.tokenize(source));
    }

    @Test
    void acceptsNegativeNonZeroNumber() {
        String source = "-5 $";
        List<Token> tokens = lexer.tokenize(source);
        assertEquals(TokenType.NUM, tokens.get(0).type());
    }

    @Test
    void acceptsNegativeDecimal() {
        String source = "-0.5 $";
        List<Token> tokens = lexer.tokenize(source);
        assertEquals(TokenType.NUM, tokens.get(0).type());
    }

    @Test
    void rejectsLoneMinusSign() {
        String source = "- $";
        assertThrows(Lexer.LexException.class, () -> lexer.tokenize(source));
    }

    @Test
    void rejectsNumberImmediatelyFollowedByParen() {
        String source = "add ( 5) $";  // "5)" -- no space before ')'
        assertThrows(Lexer.LexException.class, () -> lexer.tokenize(source));
    }

    @Test
    void rejectsStringImmediatelyFollowedBySemicolon() {
        String source = "print \"hi\"; $";  // no space before ';'
        assertThrows(Lexer.LexException.class, () -> lexer.tokenize(source));
    }
}