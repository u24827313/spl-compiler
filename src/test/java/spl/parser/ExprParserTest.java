package spl.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import spl.lexer.Lexer;
import spl.lexer.Token;
import spl.lexer.TokenType;
import spl.tree.Node;
import spl.tree.SyntaxTree;

/**
 * Unit and integration tests for the expression rules. The integration cases
 * deliberately cross into InstrParser and ProgramParser to verify the contracts
 * used by calls, branches, loops, and complete programs.
 *
 * <p>The tests build tokens directly instead of invoking the lexer. This keeps
 * each test focused on parser behavior and makes the exact token sequence being
 * parsed visible in the test itself.</p>
 */
class ExprParserTest {

    @Test
    void lookaheadOverloadDoesNotMoveCurrentPosition() {
        ParserContext ctx = context(
                token(TokenType.NUM, "1_"),
                token(TokenType.EOF, "$")
        );

        assertEquals(TokenType.NUM, ctx.peek().type());
        assertEquals(TokenType.EOF, ctx.peek(1).type());
        assertEquals(TokenType.NUM, ctx.peek().type());
    }

    @Test
    void parsesUserDefinedNameAsTermWhenNotFollowedByLeftParenthesis() {
        ParserContext ctx = context(
                token(TokenType.USER_DEFINED_NAME, "#x"),
                token(TokenType.EOF, "$")
        );

        Node term = ExprParser.parseTerm(ctx);

        assertPreorder(term, "TERM", "#x");
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(term);
    }

    @Test
    void parsesUserDefinedNameFollowedByLeftParenthesisAsCallTerm() {
        ParserContext ctx = context(
                token(TokenType.USER_DEFINED_NAME, "#function"),
                token(TokenType.LPAREN, "("),
                token(TokenType.RPAREN, ")"),
                token(TokenType.EOF, "$")
        );

        Node term = ExprParser.parseTerm(ctx);

        assertPreorder(term, "TERM", "CALL", "#function", "(", "INPUT", ")");
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(term);
    }

    @Test
    void parsesNestedBinaryAndUnaryTerms() {
        ParserContext ctx = context(
                token(TokenType.ADD, "add"),
                token(TokenType.LPAREN, "("),
                token(TokenType.NUM, "1_"),
                token(TokenType.NEG, "neg"),
                token(TokenType.LPAREN, "("),
                token(TokenType.USER_DEFINED_NAME, "#x"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.EOF, "$")
        );

        Node term = ExprParser.parseTerm(ctx);

        assertPreorder(
                term,
                "TERM", "add", "(",
                "TERM", "1_",
                "TERM", "neg", "(", "TERM", "#x", ")",
                ")"
        );
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(term);
    }

    @Test
    void parsesEqualityBetweenTerms() {
        ParserContext ctx = context(
                token(TokenType.EQ, "eq"),
                token(TokenType.LPAREN, "("),
                token(TokenType.USER_DEFINED_NAME, "#x"),
                token(TokenType.NUM, "10_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.EOF, "$")
        );

        Node bool = ExprParser.parseBool(ctx);

        assertPreorder(bool, "BOOL", "eq", "(", "TERM", "#x", "TERM", "10_", ")");
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(bool);
    }

    @Test
    void parsesNestedBooleanExpression() {
        ParserContext ctx = context(
                token(TokenType.AND, "and"),
                token(TokenType.LPAREN, "("),
                token(TokenType.LARGER, "larger"),
                token(TokenType.LPAREN, "("),
                token(TokenType.USER_DEFINED_NAME, "#x"),
                token(TokenType.NUM, "0_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.NOT, "not"),
                token(TokenType.LPAREN, "("),
                token(TokenType.LESSER, "lesser"),
                token(TokenType.LPAREN, "("),
                token(TokenType.USER_DEFINED_NAME, "#y"),
                token(TokenType.NUM, "10_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.EOF, "$")
        );

        Node bool = ExprParser.parseBool(ctx);

        assertPreorder(
                bool,
                "BOOL", "and", "(",
                "BOOL", "larger", "(", "TERM", "#x", "TERM", "0_", ")",
                "BOOL", "not", "(",
                "BOOL", "lesser", "(", "TERM", "#y", "TERM", "10_", ")",
                ")",
                ")"
        );
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(bool);
    }

    @Test
    void parsesWhileAndUntilConditions() {
        ParserContext whileContext = context(
                token(TokenType.WHILE, "while"),
                token(TokenType.EOF, "$")
        );
        ParserContext untilContext = context(
                token(TokenType.UNTIL, "until"),
                token(TokenType.EOF, "$")
        );

        Node whileCond = ExprParser.parseCond(whileContext);
        Node untilCond = ExprParser.parseCond(untilContext);

        assertPreorder(whileCond, "COND", "while");
        assertPreorder(untilCond, "COND", "until");
        assertEquals(TokenType.EOF, whileContext.peek().type());
        assertEquals(TokenType.EOF, untilContext.peek().type());
    }

    @Test
    void parsesAssignmentInstructionUsingExpressionTerm() {
        ParserContext ctx = context(
                token(TokenType.USER_DEFINED_NAME, "#x"),
                token(TokenType.EQUALS, "="),
                token(TokenType.NEG, "neg"),
                token(TokenType.LPAREN, "("),
                token(TokenType.NUM, "1_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.EOF, "$")
        );

        Node instruction = InstrParser.parseInstr(ctx);

        assertPreorder(
                instruction,
                "INSTR", "ASSIGN", "#x", "=",
                "TERM", "neg", "(", "TERM", "1_", ")"
        );
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(instruction);
    }

    @Test
    void parsesProgramOutputContainingExpressionTerm() {
        ParserContext ctx = context(
                token(TokenType.LPAREN, "("),
                token(TokenType.ADD, "add"),
                token(TokenType.LPAREN, "("),
                token(TokenType.NUM, "1_"),
                token(TokenType.NUM, "2_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.EOF, "$")
        );

        Node output = ProgramParser.parseOutp(ctx);

        assertPreorder(
                output,
                "OUTP", "(",
                "TERM", "add", "(", "TERM", "1_", "TERM", "2_", ")",
                ")"
        );
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(output);
    }

    @Test
    void parsesNumericFunctionReturnExpressionUsingExpressionTerm() {
        ParserContext ctx = context(
                token(TokenType.NUM_KW, "num"),
                token(TokenType.USER_DEFINED_NAME, "#function"),
                token(TokenType.LPAREN, "("),
                token(TokenType.RPAREN, ")"),
                token(TokenType.LBRACE, "{"),
                token(TokenType.COLON, ":"),
                token(TokenType.COLON, ":"),
                token(TokenType.RETURN, "return"),
                token(TokenType.LPAREN, "("),
                token(TokenType.NEG, "neg"),
                token(TokenType.LPAREN, "("),
                token(TokenType.NUM, "1_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.RBRACE, "}"),
                token(TokenType.EOF, "$")
        );

        Node function = ProgramParser.parseFType(ctx);
        List<String> contents = new ArrayList<>();
        collectPreorder(function, contents);

        assertEquals("F_TYPE", function.getContents());
        assertTrue(contents.contains("TERM"));
        assertTrue(contents.contains("neg"));
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(function);
    }

    @Test
    void parsesBranchUsingProgramParserAlgoForBothBodies() {
        ParserContext ctx = context(
                token(TokenType.IF, "if"),
                token(TokenType.EQ, "eq"),
                token(TokenType.LPAREN, "("),
                token(TokenType.NUM, "1_"),
                token(TokenType.NUM, "1_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.THEN, "then"),
                token(TokenType.LBRACE, "{"),
                token(TokenType.RBRACE, "}"),
                token(TokenType.ELSE, "else"),
                token(TokenType.LBRACE, "{"),
                token(TokenType.RBRACE, "}"),
                token(TokenType.EOF, "$")
        );

        Node branch = ExprParser.parseBranch(ctx);

        assertPreorder(
                branch,
                "BRANCH", "if",
                "BOOL", "eq", "(", "TERM", "1_", "TERM", "1_", ")",
                "then", "{", "ALGO", "}",
                "else", "{", "ALGO", "}"
        );
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(branch);
    }

    @Test
    void parsesPreTestLoopContainingInstructionParserInstruction() {
        ParserContext ctx = context(
                token(TokenType.WHILE, "while"),
                token(TokenType.LESSER, "lesser"),
                token(TokenType.LPAREN, "("),
                token(TokenType.USER_DEFINED_NAME, "#x"),
                token(TokenType.NUM, "10_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.DO, "do"),
                token(TokenType.LBRACE, "{"),
                token(TokenType.NOP, "nop"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.RBRACE, "}"),
                token(TokenType.EOF, "$")
        );

        Node loop = ExprParser.parseLoop(ctx);

        assertPreorder(
                loop,
                "LOOP", "COND", "while",
                "BOOL", "lesser", "(", "TERM", "#x", "TERM", "10_", ")",
                "do", "{", "ALGO", "INSTR", "nop", ";", "ALGO", "}"
        );
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(loop);
    }

    @Test
    void parsesPostTestLoopContainingInstructionParserInstruction() {
        ParserContext ctx = context(
                token(TokenType.DO, "do"),
                token(TokenType.LBRACE, "{"),
                token(TokenType.NOP, "nop"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.RBRACE, "}"),
                token(TokenType.UNTIL, "until"),
                token(TokenType.EQ, "eq"),
                token(TokenType.LPAREN, "("),
                token(TokenType.NUM, "1_"),
                token(TokenType.NUM, "1_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.EOF, "$")
        );

        Node loop = ExprParser.parseLoop(ctx);

        assertPreorder(
                loop,
                "LOOP", "do", "{", "ALGO", "INSTR", "nop", ";", "ALGO", "}",
                "COND", "until",
                "BOOL", "eq", "(", "TERM", "1_", "TERM", "1_", ")"
        );
        assertEquals(TokenType.EOF, ctx.peek().type());
        assertTreeLinksAndUniqueIds(loop);
    }

    @Test
    void parsesExpressionProgramFromRealLexerTokens() {
        String source = ": : if eq ( add ( 1 2 ) neg ( 3 ) ) "
                + "then { nop ; } else { nop ; } ; $";

        List<Token> tokens = new Lexer().tokenize(source);
        SyntaxTree tree = new Parser().parse(tokens);
        List<String> contents = new ArrayList<>();
        collectPreorder(tree.getRoot(), contents);

        assertEquals("SPL_PROG", tree.getRoot().getContents());
        assertTrue(contents.contains("BRANCH"));
        assertTrue(contents.contains("eq"));
        assertTrue(contents.contains("add"));
        assertTrue(contents.contains("neg"));
        assertTreeLinksAndUniqueIds(tree.getRoot());
    }

    @Test
    void parsesCompleteProgramAcrossAllThreeParserClasses() {
        List<Token> tokens = List.of(
                token(TokenType.COLON, ":"),
                token(TokenType.COLON, ":"),
                token(TokenType.IF, "if"),
                token(TokenType.EQ, "eq"),
                token(TokenType.LPAREN, "("),
                token(TokenType.NUM, "1_"),
                token(TokenType.NUM, "1_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.THEN, "then"),
                token(TokenType.LBRACE, "{"),
                token(TokenType.NOP, "nop"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.RBRACE, "}"),
                token(TokenType.ELSE, "else"),
                token(TokenType.LBRACE, "{"),
                token(TokenType.NOP, "nop"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.RBRACE, "}"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.EOF, "$")
        );

        SyntaxTree tree = new Parser().parse(tokens);
        Node root = tree.getRoot();
        List<String> contents = new ArrayList<>();
        collectPreorder(root, contents);

        assertEquals("SPL_PROG", root.getContents());
        assertEquals("$", root.getChildren().get(1).getContents());
        assertTrue(contents.contains("BRANCH"));
        assertTrue(contents.contains("BOOL"));
        assertTrue(contents.contains("ALGO"));
        assertTreeLinksAndUniqueIds(root);
    }

    @Test
    void rejectsMissingSecondBinaryTerm() {
        ParserContext ctx = context(
                token(TokenType.ADD, "add"),
                token(TokenType.LPAREN, "("),
                token(TokenType.NUM, "1_"),
                token(TokenType.RPAREN, ")"),
                token(TokenType.EOF, "$")
        );

        ParseException error = assertThrows(ParseException.class, () -> ExprParser.parseTerm(ctx));

        assertEquals("Missing term or expression", error.getDescription());
        assertTrue(error.getMessage().contains("Expected a term"));
        assertTrue(error.getMessage().contains("')'"));
    }

    @Test
    void rejectsNonBooleanTokenAtStartOfBooleanExpression() {
        ParserContext ctx = context(
                token(TokenType.NUM, "1_"),
                token(TokenType.EOF, "$")
        );

        ParseException error = assertThrows(ParseException.class, () -> ExprParser.parseBool(ctx));

        assertEquals("Invalid boolean expression", error.getDescription());
        assertTrue(error.getMessage().contains("Expected a boolean expression"));
        assertEquals(1, error.getLine());
        assertEquals(1, error.getColumn());
    }

    /** Creates the shared parser state around the exact tokens supplied by a test. */
    private static ParserContext context(Token... tokens) {
        return new ParserContext(List.of(tokens));
    }

    /**
     * Creates a token at a fixed source position. Tests concerned with error
     * locations can therefore expect the stable location {@code line 1, column 1}.
     */
    private static Token token(TokenType type, String lexeme) {
        return new Token(type, lexeme, 1, 1);
    }

    /**
     * Compares a tree through preorder traversal: visit the current node first,
     * then recursively visit each child from left to right. Preorder makes the
     * expected grammar expansion compact while still checking child ordering.
     */
    private static void assertPreorder(Node root, String... expectedContents) {
        List<String> actualContents = new ArrayList<>();
        collectPreorder(root, actualContents);
        assertEquals(List.of(expectedContents), actualContents);
    }

    /** Recursive worker used by {@link #assertPreorder(Node, String...)}. */
    private static void collectPreorder(Node node, List<String> contents) {
        contents.add(node.getContents());
        for (Node child : node.getChildren()) {
            collectPreorder(child, contents);
        }
    }

    /** Starts validation of the structural metadata required by tree.xml. */
    private static void assertTreeLinksAndUniqueIds(Node root) {
        Set<Integer> ids = new HashSet<>();
        assertTreeLinksAndUniqueIds(root, ids);
    }

    /**
     * Walks the complete subtree and verifies two important invariants:
     * every node ID is unique and every child points back to its actual parent.
     */
    private static void assertTreeLinksAndUniqueIds(Node node, Set<Integer> ids) {
        assertTrue(ids.add(node.getId()), "Duplicate node ID: " + node.getId());

        for (Node child : node.getChildren()) {
            assertSame(node, child.getParent());
            assertTreeLinksAndUniqueIds(child, ids);
        }
    }
}
