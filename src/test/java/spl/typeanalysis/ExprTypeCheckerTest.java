package spl.typeanalysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import spl.tree.Node;

import static org.junit.jupiter.api.Assertions.*;

class ExprTypeCheckerTest {
    private TypeContext ctx;
    private int nextId;

    @BeforeEach
    void setUp() {
        ctx = new TypeContext();
        nextId = 0;

        // Simulate types assigned by ProgramTypeChecker
        ctx.setNameType("#x", Type.NUMERIC);
        ctx.setNameType("#function", Type.NUMERIC);
        ctx.setNameType("#procedure", Type.PROCEDURE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "12", "-5", "9.01"})
    void acceptsNumericLiteral(String value) {
        assertEquals(Type.NUMERIC, ctx.check(term(value)));
    }

    @Test
    void acceptsNumericVariableAndRecordsTypes() {
        Node name = node("#x");
        Node expression = node("TERM", name);

        assertEquals(Type.NUMERIC, ctx.check(expression));
        assertEquals(Type.NUMERIC, ctx.typeOf(expression));
        assertEquals(Type.NUMERIC, ctx.typeOf(name));
    }

    @Test
    void rejectsUnknownVariable() {
        assertThrows(TypeException.class,
                () -> ctx.check(term("#missing")));
    }

    @Test
    void acceptsNumericFunctionAsTerm() {
        Node expression = node("TERM", call("#function"));

        assertEquals(Type.NUMERIC, ctx.check(expression));
    }

    @Test
    void rejectsProcedureAsTerm() {
        Node expression = node("TERM", call("#procedure"));

        assertThrows(TypeException.class, () -> ctx.check(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mod", "div", "add", "sub", "mul"})
    void acceptsNumericArithmetic(String operator) {
        Node expression = binary("TERM", operator, term("8"), term("2"));
        assertEquals(Type.NUMERIC, ctx.check(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mod", "div", "add", "sub", "mul"})
    void rejectsProcedureInEitherArithmeticOperand(String operator) {
        Node badLeft = binary("TERM", operator, node("TERM", call("#procedure")), term("2"));
        Node badRight = binary("TERM", operator, term("8"), node("TERM", call("#procedure")));

        assertThrows(TypeException.class, () -> ctx.check(badLeft));
        assertThrows(TypeException.class, () -> ctx.check(badRight));
    }

    @Test
    void acceptsNumericNegation() {
        assertEquals(Type.NUMERIC, ctx.check(unary("TERM", "neg", term("#x"))));
    }

    @Test
    void rejectsProcedureInNegation() {
        Node expression = unary("TERM", "neg", node("TERM", call("#procedure")));

        assertThrows(TypeException.class, () -> ctx.check(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"eq", "larger", "lesser"})
    void acceptsNumericComparison(String operator) {
        Node expression = binary("BOOL", operator, term("1"), term("2"));

        assertEquals(Type.BOOLEAN, ctx.check(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"eq", "larger", "lesser"})
    void rejectsProcedureInEitherComparisonOperand(String operator) {
        Node badLeft = binary("BOOL", operator, node("TERM", call("#procedure")), term("2"));
        Node badRight = binary("BOOL", operator, term("1"), node("TERM", call("#procedure")));

        assertThrows(TypeException.class, () -> ctx.check(badLeft));
        assertThrows(TypeException.class, () -> ctx.check(badRight));
    }

    @Test
    void acceptsBooleanNegation() {
        assertEquals(Type.BOOLEAN, ctx.check(unary("BOOL", "not", comparison())));
    }

    @Test
    void rejectsNumericOperandForNot() {
        Node expression = unary("BOOL", "not", term("1"));

        assertThrows(TypeException.class, () -> ctx.check(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"and", "or"})
    void acceptsBooleanOperands(String operator) {
        Node expression = binary("BOOL", operator, comparison(), comparison());

        assertEquals(Type.BOOLEAN, ctx.check(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"and", "or"})
    void rejectsNumericOperandOnEitherSide(String operator) {
        Node badLeft = binary("BOOL", operator, term("1"), comparison());
        Node badRight = binary("BOOL", operator, comparison(), term("1"));

        assertThrows(TypeException.class, () -> ctx.check(badLeft));
        assertThrows(TypeException.class, () -> ctx.check(badRight));
    }

    @Test
    void callReturnsDeclaredFunctionType() {
        assertEquals(Type.NUMERIC, ctx.check(call("#function")));
        assertEquals(Type.PROCEDURE, ctx.check(call("#procedure")));
    }

    @Test
    void rejectsUnknownFunction() {
        TypeException error = assertThrows(TypeException.class,
                () -> ctx.check(call("#missing")));

        assertTrue(error.getMessage().contains("unknown"));
    }

    @Test
    void acceptsEmptyInput() {
        assertEquals(Type.OK, ctx.check(input()));
    }

    @Test
    void acceptsMultipleNumericArguments() {
        Node arguments = input(term("1"), term("#x"), node("TERM", call("#function")));

        assertEquals(Type.OK, ctx.check(arguments));
    }

    @Test
    void rejectsProcedureInLaterArgument() {
        Node arguments = input(term("1"), node("TERM", call("#procedure")));

        assertThrows(TypeException.class, () -> ctx.check(arguments));
    }

    @Test
    void callChecksItsArguments() {
        Node expression = call("#function", term("1"), node("TERM", call("#procedure")));

        assertThrows(TypeException.class, () -> ctx.check(expression));
    }

    @Test
    void acceptsNestedArithmeticAndCalls() {
        Node expression = binary("TERM", "add",
                unary("TERM", "neg", term("#x")),
                node("TERM", call("#function", term("2"))));

        assertEquals(Type.NUMERIC, ctx.check(expression));
    }

    // Helpers will build trees with the same child order as the parser

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

    private Node unary(String kind, String operator, Node operand) {
        return node(kind, node(operator), node("("), operand, node(")"));
    }

    private Node binary(
        String kind, String operator, Node left, Node right) {
        return node(kind, node(operator), node("("), left, right, node(")"));
    }

    private Node comparison() {
        return binary("BOOL", "eq", term("1"), term("2"));
    }

    private Node call(String name, Node... arguments) {
        return node("CALL", node(name), node("("), input(arguments), node(")"));
    }

    private Node input(Node... arguments) {
        Node rest = node("INPUT");
        for (int i = arguments.length - 1; i >= 0; i--) {
            rest = node("INPUT", arguments[i], rest);
        }
        return rest;
    }
}