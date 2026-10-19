package spl.typeanalysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import spl.tree.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstrTypeCheckerTest {
    private TypeContext ctx;
    private int nextId;

    @BeforeEach
    void setUp() {
        ctx = new TypeContext();
        nextId = 0;

        // Simulate declaration types assigned by ProgramTypeChecker.
        ctx.setNameType("#x", Type.NUMERIC);
        ctx.setNameType("#function", Type.NUMERIC);
        ctx.setNameType("#procedure", Type.PROCEDURE);
    }

    @Test
    void acceptsWhileConditionAndRecordsOkType() {
        Node keyword = node("while");
        Node condition = node("COND", keyword);

        assertEquals(Type.OK, ctx.check(condition));
        assertEquals(Type.OK, ctx.typeOf(condition));
        assertEquals(Type.UNKNOWN, ctx.typeOf(keyword));
    }

    @Test
    void acceptsUntilCondition() {
        assertEquals(Type.OK, ctx.check(node("COND", node("until"))));
    }

    @Test
    void rejectsConditionWithoutKeyword() {
        Node condition = node("COND");

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(condition));

        assertEquals("Malformed COND: expected 1 children but found 0", error.getMessage());
        assertSame(condition, error.getNode());
    }

    @Test
    void rejectsUnsupportedConditionKeyword() {
        Node condition = node("COND", node("if"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(condition));

        assertEquals("Malformed COND: expected while or until but found if", error.getMessage());
        assertSame(condition, error.getNode());
    }

    @Test
    void rejectsConditionWithExtraChild() {
        Node condition = node("COND", node("while"), node("until"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(condition));

        assertEquals("Malformed COND: expected 1 children but found 2", error.getMessage());
        assertSame(condition, error.getNode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"hello\"", "\"hello.world\""})
    void acceptsStringOutputAndRecordsBothNodeTypes(String contents) {
        Node string = node(contents);
        Node output = node("OUTP", string);

        assertEquals(Type.OK, ctx.check(output));

        assertEquals(Type.OK, ctx.typeOf(output));
        assertEquals(Type.OK, ctx.typeOf(string));
    }

    @Test
    void acceptsNumericLiteralOutputAndRecordsTypes() {
        Node literal = node("5");
        Node expression = node("TERM", literal);
        Node output = output(expression);

        assertEquals(Type.OK, ctx.check(output));

        assertEquals(Type.OK, ctx.typeOf(output));
        assertEquals(Type.NUMERIC, ctx.typeOf(expression));
        assertEquals(Type.NUMERIC, ctx.typeOf(literal));
    }

    @Test
    void acceptsNumericVariableOutput() {
        Node expression = term("#x");

        assertEquals(Type.OK, ctx.check(output(expression)));
        assertEquals(Type.NUMERIC, ctx.typeOf(expression));
    }

    @Test
    void acceptsArithmeticOutput() {
        Node expression = binaryTerm("add", term("5"), term("2"));

        assertEquals(Type.OK, ctx.check(output(expression)));
        assertEquals(Type.NUMERIC, ctx.typeOf(expression));
    }

    @Test
    void acceptsNumericFunctionCallOutput() {
        Node call = call("#function");
        Node expression = node("TERM", call);
        Node output = output(expression);

        assertEquals(Type.OK, ctx.check(output));

        assertEquals(Type.OK, ctx.typeOf(output));
        assertEquals(Type.NUMERIC, ctx.typeOf(expression));
        assertEquals(Type.NUMERIC, ctx.typeOf(call));
    }

    @Test
    void rejectsProcedureCallAsOutputTerm() {
        Node call = call("#procedure");
        Node expression = node("TERM", call);
        Node output = output(expression);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(output));

        assertEquals("Type error in rule TERM -> CALL: expected numeric but found procedure",
                error.getMessage());
        assertSame(call, error.getNode());
    }

    @Test
    void rejectsBooleanInNumericOutputPosition() {
        Node booleanExpression = comparison();
        Node output = output(booleanExpression);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(output));

        assertEquals("Type error in rule OUTP -> ( TERM ): expected numeric but found boolean",
                error.getMessage());
        assertSame(booleanExpression, error.getNode());
    }

    @Test
    void rejectsOutputWithoutChildren() {
        Node output = node("OUTP");

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(output));

        assertEquals("Malformed OUTP: expected [STRING] or [(, TERM, )] but found 0 children",
                error.getMessage());
        assertSame(output, error.getNode());
    }

    @Test
    void rejectsSingleNonStringOutputChild() {
        Node output = node("OUTP", node("while"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(output));

        assertEquals("Malformed OUTP: expected STRING but found while", error.getMessage());
        assertSame(output, error.getNode());
    }

    @Test
    void rejectsOutputWithUnsupportedChildCount() {
        Node output = node("OUTP", node("("), term("5"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(output));

        assertEquals("Malformed OUTP: expected [STRING] or [(, TERM, )] but found 2 children",
                error.getMessage());
        assertSame(output, error.getNode());
    }

    @Test
    void rejectsOutputWithMalformedParentheses() {
        Node output = node("OUTP", node("("), term("5"), node("]"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(output));

        assertEquals("Malformed OUTP: expected ( TERM )", error.getMessage());
        assertSame(output, error.getNode());
    }

    private Node node(String contents, Node... children) {
        Node result = new Node(nextId++, contents);
        for (Node child : children) {
            result.addChild(child);
        }
        return result;
    }

    private Node term(String token) {
        return node("TERM", node(token));
    }

    private Node binaryTerm(String operator, Node left, Node right) {
        return node("TERM", node(operator), node("("), left, right, node(")"));
    }

    private Node comparison() {
        return node("BOOL", node("eq"), node("("), term("1"), term("2"), node(")"));
    }

    private Node call(String name) {
        return node("CALL", node(name), node("("), node("INPUT"), node(")"));
    }

    private Node output(Node expression) {
        return node("OUTP", node("("), expression, node(")"));
    }
}
