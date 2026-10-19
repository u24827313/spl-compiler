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

    @Test
    void acceptsNumericAssignmentAndRecordsTypesWithoutChangingDeclaration() {
        Node name = node("#x");
        Node expression = term("5");
        Node assignment = node("ASSIGN", name, node("="), expression);

        assertEquals(Type.OK, ctx.check(assignment));

        assertEquals(Type.OK, ctx.typeOf(assignment));
        assertEquals(Type.NUMERIC, ctx.typeOf(name));
        assertEquals(Type.NUMERIC, ctx.typeOf(expression));
        assertEquals(Type.NUMERIC, ctx.nameType("#x"));
    }

    @Test
    void acceptsArithmeticAssignmentValue() {
        Node assignment = assignment("#x", binaryTerm("add", term("5"), term("2")));

        assertEquals(Type.OK, ctx.check(assignment));
    }

    @Test
    void acceptsNumericFunctionAssignmentValue() {
        Node assignment = assignment("#x", node("TERM", call("#function")));

        assertEquals(Type.OK, ctx.check(assignment));
    }

    @Test
    void rejectsUnknownAssignmentTargetWithoutDeclaringIt() {
        Node name = node("#missing");
        Node assignment = node("ASSIGN", name, node("="), term("5"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Type error in rule ASSIGN -> NAME = TERM: expected numeric but found unknown",
                error.getMessage());
        assertSame(name, error.getNode());
        assertEquals(Type.UNKNOWN, ctx.nameType("#missing"));
    }

    @Test
    void rejectsProcedureAsAssignmentTargetWithoutChangingItsType() {
        Node name = node("#procedure");
        Node assignment = node("ASSIGN", name, node("="), term("5"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Type error in rule ASSIGN -> NAME = TERM: expected numeric but found procedure",
                error.getMessage());
        assertSame(name, error.getNode());
        assertEquals(Type.PROCEDURE, ctx.nameType("#procedure"));
    }

    @Test
    void rejectsProcedureCallAsAssignmentValue() {
        Node call = call("#procedure");
        Node assignment = assignment("#x", node("TERM", call));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Type error in rule TERM -> CALL: expected numeric but found procedure",
                error.getMessage());
        assertSame(call, error.getNode());
        assertEquals(Type.NUMERIC, ctx.nameType("#x"));
    }

    @Test
    void rejectsBooleanAssignmentValue() {
        Node booleanExpression = comparison();
        Node assignment = assignment("#x", booleanExpression);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Type error in rule ASSIGN -> NAME = TERM: expected numeric but found boolean",
                error.getMessage());
        assertSame(booleanExpression, error.getNode());
    }

    @Test
    void rejectsNonNameAssignmentTarget() {
        Node target = node("5");
        Node assignment = node("ASSIGN", target, node("="), term("5"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Malformed ASSIGN: expected NAME but found NUM", error.getMessage());
        assertSame(target, error.getNode());
    }

    @Test
    void rejectsAssignmentWithMalformedEqualsToken() {
        Node assignment = node("ASSIGN", node("#x"), node("+"), term("5"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Malformed ASSIGN: expected '=' between NAME and TERM", error.getMessage());
        assertSame(assignment, error.getNode());
    }

    @Test
    void rejectsAssignmentWithWrongChildCount() {
        Node assignment = node("ASSIGN", node("#x"), node("="));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Malformed ASSIGN: expected 3 children but found 2", error.getMessage());
        assertSame(assignment, error.getNode());
    }

    @Test
    void acceptsNoOpInstructionAndRecordsOkType() {
        Node instruction = node("INSTR", node("nop"));

        assertEquals(Type.OK, ctx.check(instruction));
        assertEquals(Type.OK, ctx.typeOf(instruction));
    }

    @Test
    void acceptsCommentInstructionAndRecordsStringType() {
        Node string = node("\"note\"");
        Node instruction = node("INSTR", node("comment"), string);

        assertEquals(Type.OK, ctx.check(instruction));

        assertEquals(Type.OK, ctx.typeOf(instruction));
        assertEquals(Type.OK, ctx.typeOf(string));
    }

    @Test
    void rejectsCommentInstructionWithNonStringPayload() {
        Node instruction = node("INSTR", node("comment"), node("5"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(instruction));

        assertEquals("Malformed INSTR: expected STRING after comment but found NUM",
                error.getMessage());
        assertSame(instruction, error.getNode());
    }

    @Test
    void acceptsPrintInstructionAndRecordsOutputTypes() {
        Node output = output(term("5"));
        Node instruction = node("INSTR", node("print"), output);

        assertEquals(Type.OK, ctx.check(instruction));

        assertEquals(Type.OK, ctx.typeOf(instruction));
        assertEquals(Type.OK, ctx.typeOf(output));
    }

    @Test
    void rejectsPrintInstructionWithoutOutpChild() {
        Node instruction = node("INSTR", node("print"), node("\"text\""));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(instruction));

        assertEquals("Malformed INSTR: expected OUTP after print but found STRING",
                error.getMessage());
        assertSame(instruction, error.getNode());
    }

    @Test
    void acceptsAssignmentInstructionAndRecordsOkType() {
        Node assignment = assignment("#x", term("5"));
        Node instruction = node("INSTR", assignment);

        assertEquals(Type.OK, ctx.check(instruction));

        assertEquals(Type.OK, ctx.typeOf(instruction));
        assertEquals(Type.OK, ctx.typeOf(assignment));
    }

    @Test
    void acceptsProcedureCallInstructionAndRecordsParentAsOk() {
        Node call = call("#procedure");
        Node instruction = node("INSTR", call);

        assertEquals(Type.OK, ctx.check(instruction));

        assertEquals(Type.OK, ctx.typeOf(instruction));
        assertEquals(Type.PROCEDURE, ctx.typeOf(call));
    }

    @Test
    void rejectsNumericFunctionCallAsInstruction() {
        Node call = call("#function");
        Node instruction = node("INSTR", call);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(instruction));

        assertEquals("Type error in rule INSTR -> CALL: expected procedure but found numeric",
                error.getMessage());
        assertSame(call, error.getNode());
    }

    @Test
    void propagatesUnknownCallErrorFromExpressionChecker() {
        Node name = node("#missing");
        Node call = node("CALL", name, node("("), node("INPUT"), node(")"));
        Node instruction = node("INSTR", call);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(instruction));

        assertEquals("CALL requires a numeric function or procedure, but found unknown",
                error.getMessage());
        assertSame(name, error.getNode());
    }

    @Test
    void propagatesInvalidCallArgumentError() {
        Node invalidArgumentCall = call("#procedure");
        Node argument = node("TERM", invalidArgumentCall);
        Node outerCall = call("#function", argument);
        Node instruction = node("INSTR", outerCall);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(instruction));

        assertEquals("Type error in rule TERM -> CALL: expected numeric but found procedure",
                error.getMessage());
        assertSame(invalidArgumentCall, error.getNode());
    }

    @Test
    void rejectsInstructionWithoutChildren() {
        Node instruction = node("INSTR");

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(instruction));

        assertEquals("Malformed INSTR: expected an instruction but found no children",
                error.getMessage());
        assertSame(instruction, error.getNode());
    }

    @Test
    void rejectsUnimplementedBranchAlternative() {
        Node instruction = node("INSTR", node("BRANCH"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(instruction));

        assertEquals("Unknown INSTR alternative: BRANCH", error.getMessage());
        assertSame(instruction, error.getNode());
    }

    @Test
    void rejectsWrongChildCountsForSupportedInstructionVariants() {
        Node[] malformedInstructions = {
                node("INSTR", node("nop"), node(";")),
                node("INSTR", node("comment")),
                node("INSTR", node("print"), output(term("5")), node(";")),
                node("INSTR", node("ASSIGN"), node("ASSIGN")),
                node("INSTR", call("#procedure"), node(";"))
        };

        for (Node instruction : malformedInstructions) {
            assertThrows(TypeException.class, () -> ctx.check(instruction));
        }
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

    private Node assignment(String name, Node expression) {
        return node("ASSIGN", node(name), node("="), expression);
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

    private Node output(Node expression) {
        return node("OUTP", node("("), expression, node(")"));
    }
}
