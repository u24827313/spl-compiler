package spl.parser;
import org.junit.jupiter.api.Test;

import spl.lexer.Token;
import spl.lexer.TokenType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import spl.tree.Node;

import spl.parser.ParseException;
import java.util.List;

public class ProgramParserTest {

    private Token tok(TokenType type, String lexeme){
        return new Token(type, lexeme, 1, 1);
    }

    private ParserContext ctxFrom(List<Token> tokens){
        return new ParserContext(tokens);

    }

    @Test 
    void parseVDecl_matchedEpsilon(){
        List<Token> tokens = List.of(tok(TokenType.COLON, ":"));
        ParserContext ctx = ctxFrom(tokens);

        Node node = ProgramParser.parseVDecl(ctx);
        assertEquals("V_DECL", node.getContents());
        assertTrue(node.getChildren().isEmpty());
        assertEquals(TokenType.COLON, ctx.peek().getType());
    }

    @Test
    void parseVDecl_matchesOneName(){
        List<Token> tokens = List.of(
            tok(TokenType.USER_DEFINED_NAME, "#x"),
            tok(TokenType.COLON, ":")
        );
        ParserContext ctx = ctxFrom(tokens);
        Node node = ProgramParser.parseVDecl(ctx);

        assertEquals(2, node.getChildren().size());
        assertEquals("#x", node.getChildren().get(0).getContents());
        assertTrue(node.getChildren().get(1).getChildren().isEmpty());
    }

    @Test
    void parseVDecl_matchesMultipleNames(){
        List<Token> tokens = List.of(
            tok(TokenType.USER_DEFINED_NAME, "#x"),
            tok(TokenType.USER_DEFINED_NAME, "#y"),
            tok(TokenType.COLON, ":")
        );
        ParserContext ctx = ctxFrom(tokens);

        Node node = ProgramParser.parseVDecl(ctx);
        assertEquals("#x", node.getChildren().get(0).getContents());
        Node rest = node.getChildren().get(1);
        assertEquals("#y", rest.getChildren().get(0).getContents());
    }

    @Test
    void parseFDecl_matchesEpsilon(){
        List<Token> tokens = List.of(
            tok(TokenType.COLON, ":")
        );
        ParserContext ctx = ctxFrom(tokens);

        Node node = ProgramParser.parseFDecl(ctx);
        assertTrue(node.getChildren().isEmpty());
    }

    @Test
    void parseFType_throwsOnInvalidStart(){
        List<Token> tokens = List.of(
            tok(TokenType.PRINT, "print")
        );
        ParserContext ctx = ctxFrom(tokens);
        ParseException ex = assertThrows(ParseException.class, 
            () -> ProgramParser.parseFType(ctx)
        );

        assertTrue(ex.getMessage().contains("void") || ex.getMessage().contains("num"));
    }

    @Test
    void parseFType_voidBranch_throwsOnMissingOpenParen(){
        List<Token> tokens = List.of(
            tok(TokenType.VOID, "void"),
            tok(TokenType.USER_DEFINED_NAME, "#f"),
            tok(TokenType.LBRACE, "{")
        );
        ParserContext ctx = ctxFrom(tokens);

        assertThrows(ParseException.class, () -> ProgramParser.parseFType(ctx));
    }

    @Test
    void parseAlgo_matchedEpsilon(){
        List<Token> tokens = List.of(
            tok(TokenType.COLON, ":")
        );
        ParserContext ctx = ctxFrom(tokens);
        Node node = ProgramParser.parseAlgo(ctx);
        assertTrue(node.getChildren().isEmpty());
    }

    @Test
    void parseAlgo_matchesPrintInstruction() {
        // print "hi" ; <stop>
        List<Token> tokens = List.of(
                tok(TokenType.PRINT, "print"),
                tok(TokenType.STRING, "\"hi\""),
                tok(TokenType.SEMICOLON, ";"),
                tok(TokenType.COLON, ":") // lookahead that ends the recursive ALGO via epsilon
        );
        ParserContext ctx = ctxFrom(tokens);

        Node node = ProgramParser.parseAlgo(ctx);

        // ALGO -> INSTR ; ALGO(epsilon)  =>  3 children
        assertEquals(3, node.getChildren().size());
        // confirm the ';' was actually consumed and we stopped right before ':'
        assertEquals(TokenType.COLON, ctx.peek().getType());
    }


    @Test
    void parseOutp_matchesStringLiteral() {
        List<Token> tokens = List.of(tok(TokenType.STRING, "\"hello\""));
        ParserContext ctx = ctxFrom(tokens);

        Node node = ProgramParser.parseOutp(ctx);

        assertEquals(1, node.getChildren().size());
        assertEquals("\"hello\"", node.getChildren().get(0).getContents());
    }

    @Test
    void parseOutp_parenTerm_matchesRealTerm() {
        List<Token> tokens = List.of(
                tok(TokenType.LPAREN, "("),
                tok(TokenType.NUM, "5_"),
                tok(TokenType.RPAREN, ")")
        );
        ParserContext ctx = ctxFrom(tokens);

        Node node = ProgramParser.parseOutp(ctx);

        // OUTP -> ( TERM )  =>  3 children: '(', TERM subtree, ')'
        assertEquals(3, node.getChildren().size());
        assertEquals("(", node.getChildren().get(0).getContents());
        assertEquals(")", node.getChildren().get(2).getContents());
    }

    @Test
    void parseOutp_throwsOnInvalidStart() {
        List<Token> tokens = List.of(tok(TokenType.COLON, ":"));
        ParserContext ctx = ctxFrom(tokens);

        assertThrows(ParseException.class, () -> ProgramParser.parseOutp(ctx));
    }

    @Test
    void parseSplProg_matchesEmptyProgram() {
        List<Token> tokens = List.of(
                tok(TokenType.COLON, ":"),
                tok(TokenType.COLON, ":"),
                tok(TokenType.EOF, "$")
        );
        ParserContext ctx = ctxFrom(tokens);

        Node node = ProgramParser.parseSplProg(ctx);

        assertEquals("SPL_PROG", node.getContents());
        assertEquals(2, node.getChildren().size()); // P subtree + '$' leaf
        assertEquals("$", node.getChildren().get(1).getContents());
    }
}
