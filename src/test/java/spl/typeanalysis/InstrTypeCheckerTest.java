package spl.typeanalysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

        assertEquals("Expected a numeric value for a numeric expression, but found a procedure.",
                error.getMessage());
        assertSame(call, error.getNode());
    }

    @Test
    void rejectsBooleanInNumericOutputPosition() {
        Node booleanExpression = comparison();
        Node output = output(booleanExpression);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(output));

        assertEquals("Expected a numeric value for a print expression, but found a boolean value.",
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

        assertEquals("Expected a numeric value for an assignment, but found a value with an unknown type.",
                error.getMessage());
        assertSame(name, error.getNode());
        assertEquals(Type.UNKNOWN, ctx.nameType("#missing"));
    }

    @Test
    void rejectsProcedureAsAssignmentTargetWithoutChangingItsType() {
        Node name = node("#procedure");
        Node assignment = node("ASSIGN", name, node("="), term("5"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Expected a numeric value for an assignment, but found a procedure.",
                error.getMessage());
        assertSame(name, error.getNode());
        assertEquals(Type.PROCEDURE, ctx.nameType("#procedure"));
    }

    @Test
    void rejectsProcedureCallAsAssignmentValue() {
        Node call = call("#procedure");
        Node assignment = assignment("#x", node("TERM", call));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Expected a numeric value for a numeric expression, but found a procedure.",
                error.getMessage());
        assertSame(call, error.getNode());
        assertEquals(Type.NUMERIC, ctx.nameType("#x"));
    }

    @Test
    void rejectsBooleanAssignmentValue() {
        Node booleanExpression = comparison();
        Node assignment = assignment("#x", booleanExpression);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(assignment));

        assertEquals("Expected a numeric value for an assignment, but found a boolean value.",
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

        assertEquals("Expected a procedure for a function call used as an instruction, but found a numeric value.",
                error.getMessage());
        assertSame(call, error.getNode());
    }

    @Test
    void propagatesUnknownCallErrorFromExpressionChecker() {
        Node name = node("#missing");
        Node call = node("CALL", name, node("("), node("INPUT"), node(")"));
        Node instruction = node("INSTR", call);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(instruction));

        assertEquals("The name '#missing' cannot be called because it has type 'unknown'. Only numeric functions and procedures can be called.",
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

        assertEquals("Expected a numeric value for a numeric expression, but found a procedure.",
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
    void acceptsBranchWithEmptyAlgorithmsAndRecordsTypes() {
        Node condition = comparison();
        Node thenAlgorithm = node("ALGO");
        Node elseAlgorithm = node("ALGO");
        Node branch = branch(condition, thenAlgorithm, elseAlgorithm);

        assertEquals(Type.OK, ctx.check(branch));

        assertEquals(Type.OK, ctx.typeOf(branch));
        assertEquals(Type.BOOLEAN, ctx.typeOf(condition));
        assertEquals(Type.OK, ctx.typeOf(thenAlgorithm));
        assertEquals(Type.OK, ctx.typeOf(elseAlgorithm));
    }

    @Test
    void acceptsBranchWithNonemptyAlgorithms() {
        Node thenInstruction = noOpInstruction();
        Node elseFirstInstruction = noOpInstruction();
        Node elseSecondInstruction = noOpInstruction();
        Node thenAlgorithm = algo(thenInstruction);
        Node elseAlgorithm = algo(elseFirstInstruction, elseSecondInstruction);
        Node branch = branch(comparison(), thenAlgorithm, elseAlgorithm);

        assertEquals(Type.OK, ctx.check(branch));

        assertEquals(Type.OK, ctx.typeOf(thenInstruction));
        assertEquals(Type.OK, ctx.typeOf(elseFirstInstruction));
        assertEquals(Type.OK, ctx.typeOf(elseSecondInstruction));
        assertEquals(Type.OK, ctx.typeOf(thenAlgorithm));
        assertEquals(Type.OK, ctx.typeOf(elseAlgorithm));
    }

    @Test
    void rejectsNumericBranchCondition() {
        Node condition = term("1");
        Node branch = branch(condition, node("ALGO"), node("ALGO"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(branch));

        assertEquals(
                "Expected a boolean value for a branch condition or body, but found a numeric value.",
                error.getMessage()
        );
        assertSame(condition, error.getNode());
    }

    @Test
    void propagatesInvalidExpressionFromBranchCondition() {
        Node invalidCall = call("#procedure");
        Node condition = node("BOOL", node("eq"), node("("),
                node("TERM", invalidCall), term("1"), node(")"));
        Node branch = branch(condition, node("ALGO"), node("ALGO"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(branch));

        assertEquals("Expected a numeric value for a numeric expression, but found a procedure.",
                error.getMessage());
        assertSame(invalidCall, error.getNode());
    }

    @Test
    void rejectsInvalidInstructionInFirstBranchBody() {
        Node invalidCall = call("#function");
        Node thenAlgorithm = algo(node("INSTR", invalidCall));
        Node branch = branch(comparison(), thenAlgorithm, node("ALGO"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(branch));

        assertEquals("Expected a procedure for a function call used as an instruction, but found a numeric value.",
                error.getMessage());
        assertSame(invalidCall, error.getNode());
    }

    @Test
    void checksSecondBranchBodyEvenWhenConditionIsAlwaysTrue() {
        Node condition = comparison();
        Node firstInstruction = noOpInstruction();
        Node firstAlgorithm = algo(firstInstruction);
        Node invalidCall = call("#function");
        Node secondAlgorithm = algo(node("INSTR", invalidCall));
        Node branch = branch(condition, firstAlgorithm, secondAlgorithm);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(branch));

        assertEquals("Expected a procedure for a function call used as an instruction, but found a numeric value.",
                error.getMessage());
        assertSame(invalidCall, error.getNode());
        assertEquals(Type.BOOLEAN, ctx.typeOf(condition));
        assertEquals(Type.OK, ctx.typeOf(firstInstruction));
        assertEquals(Type.OK, ctx.typeOf(firstAlgorithm));
    }

    @Test
    void checksLaterInstructionInBranchAlgorithm() {
        Node firstInstruction = noOpInstruction();
        Node invalidCall = call("#function");
        Node thenAlgorithm = algo(firstInstruction, node("INSTR", invalidCall));
        Node branch = branch(comparison(), thenAlgorithm, node("ALGO"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(branch));

        assertEquals("Expected a procedure for a function call used as an instruction, but found a numeric value.",
                error.getMessage());
        assertSame(invalidCall, error.getNode());
        assertEquals(Type.OK, ctx.typeOf(firstInstruction));
    }

    @Test
    void acceptsNestedBranch() {
        Node innerBranch = branch(comparison(), node("ALGO"), node("ALGO"));
        Node outerThenAlgorithm = algo(node("INSTR", innerBranch));
        Node outerBranch = branch(comparison(), outerThenAlgorithm, node("ALGO"));

        assertEquals(Type.OK, ctx.check(outerBranch));

        assertEquals(Type.OK, ctx.typeOf(outerBranch));
        assertEquals(Type.OK, ctx.typeOf(innerBranch));
        assertEquals(Type.OK, ctx.typeOf(outerThenAlgorithm));
    }

    @Test
    void acceptsBranchInstructionAndRecordsBranchAndWrapperTypes() {
        Node branch = branch(comparison(), node("ALGO"), node("ALGO"));
        Node instruction = node("INSTR", branch);

        assertEquals(Type.OK, ctx.check(instruction));

        assertEquals(Type.OK, ctx.typeOf(instruction));
        assertEquals(Type.OK, ctx.typeOf(branch));
    }

    @ParameterizedTest
    @CsvSource({"true, while", "true, until", "false, while", "false, until"})
    void acceptsBothLoopLayoutsWithEitherConditionKeyword(boolean prefix, String keyword) {
        Node condition = loopCondition(keyword);
        Node booleanCondition = comparison();
        Node body = node("ALGO");
        Node loop = loop(prefix, condition, booleanCondition, body);

        assertEquals(Type.OK, ctx.check(loop));

        assertEquals(Type.OK, ctx.typeOf(loop));
        assertEquals(Type.OK, ctx.typeOf(condition));
        assertEquals(Type.BOOLEAN, ctx.typeOf(booleanCondition));
        assertEquals(Type.OK, ctx.typeOf(body));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptsNonemptyLoopBodyInBothLayouts(boolean prefix) {
        Node instruction = noOpInstruction();
        Node body = algo(instruction);
        Node loop = loop(prefix, loopCondition("while"), comparison(), body);

        assertEquals(Type.OK, ctx.check(loop));

        assertEquals(Type.OK, ctx.typeOf(loop));
        assertEquals(Type.OK, ctx.typeOf(body));
        assertEquals(Type.OK, ctx.typeOf(instruction));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsNumericConditionInBothLoopLayouts(boolean prefix) {
        Node condition = term("1");
        Node loop = loop(prefix, loopCondition("while"), condition, node("ALGO"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(loop));

        assertEquals(
                "Expected a boolean value for a loop condition or body, but found a numeric value.",
                error.getMessage()
        );
        assertSame(condition, error.getNode());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void propagatesInvalidBooleanExpressionInBothLoopLayouts(boolean prefix) {
        Node invalidCall = call("#procedure");
        Node condition = node("BOOL", node("eq"), node("("),
                node("TERM", invalidCall), term("1"), node(")"));
        Node loop = loop(prefix, loopCondition("until"), condition, node("ALGO"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(loop));

        assertEquals("Expected a numeric value for a numeric expression, but found a procedure.",
                error.getMessage());
        assertSame(invalidCall, error.getNode());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsInvalidInstructionInBothLoopBodies(boolean prefix) {
        Node invalidCall = call("#function");
        Node body = algo(node("INSTR", invalidCall));
        Node loop = loop(prefix, loopCondition("while"), comparison(), body);

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(loop));

        assertEquals("Expected a procedure for a function call used as an instruction, but found a numeric value.",
                error.getMessage());
        assertSame(invalidCall, error.getNode());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsInvalidConditionKeywordInBothLoopLayouts(boolean prefix) {
        Node condition = loopCondition("if");
        Node loop = loop(prefix, condition, comparison(), node("ALGO"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(loop));

        assertEquals("Malformed COND: expected while or until but found if", error.getMessage());
        assertSame(condition, error.getNode());
    }

    @Test
    void acceptsNestedLoops() {
        Node innerLoop = postfixLoop(algo(noOpInstruction()), loopCondition("until"), comparison());
        Node outerBody = algo(node("INSTR", innerLoop));
        Node outerLoop = prefixLoop(loopCondition("while"), comparison(), outerBody);

        assertEquals(Type.OK, ctx.check(outerLoop));

        assertEquals(Type.OK, ctx.typeOf(outerLoop));
        assertEquals(Type.OK, ctx.typeOf(innerLoop));
        assertEquals(Type.OK, ctx.typeOf(outerBody));
    }

    @Test
    void acceptsBranchInsideLoop() {
        Node nestedBranch = branch(comparison(), node("ALGO"), node("ALGO"));
        Node body = algo(node("INSTR", nestedBranch));
        Node loop = prefixLoop(loopCondition("while"), comparison(), body);

        assertEquals(Type.OK, ctx.check(loop));

        assertEquals(Type.OK, ctx.typeOf(loop));
        assertEquals(Type.OK, ctx.typeOf(nestedBranch));
    }

    @Test
    void acceptsLoopInsideBranch() {
        Node nestedLoop = postfixLoop(node("ALGO"), loopCondition("until"), comparison());
        Node thenAlgorithm = algo(node("INSTR", nestedLoop));
        Node branch = branch(comparison(), thenAlgorithm, node("ALGO"));

        assertEquals(Type.OK, ctx.check(branch));

        assertEquals(Type.OK, ctx.typeOf(branch));
        assertEquals(Type.OK, ctx.typeOf(nestedLoop));
    }

    @Test
    void acceptsLoopInstructionAndRecordsLoopAndWrapperTypes() {
        Node loop = prefixLoop(loopCondition("until"), comparison(), node("ALGO"));
        Node instruction = node("INSTR", loop);

        assertEquals(Type.OK, ctx.check(instruction));

        assertEquals(Type.OK, ctx.typeOf(instruction));
        assertEquals(Type.OK, ctx.typeOf(loop));
    }

    @Test
    void rejectsLoopWithUnsupportedFirstChildKind() {
        Node loop = node("LOOP", node("while"), comparison(), node("do"), node("{"),
                node("ALGO"), node("}"));

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(loop));

        assertEquals("Malformed LOOP: expected COND or do at first child but found while",
                error.getMessage());
        assertSame(loop, error.getNode());
    }

    @Test
    void rejectsLoopWithIncorrectChildCount() {
        Node loop = node("LOOP");

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(loop));

        assertEquals("Malformed LOOP: expected 6 children but found 0", error.getMessage());
        assertSame(loop, error.getNode());
    }

    @Test
    void rejectsBranchWithIncorrectChildCount() {
        Node branch = node("BRANCH");

        TypeException error = assertThrows(TypeException.class, () -> ctx.check(branch));

        assertEquals("Malformed BRANCH: expected 10 children but found 0", error.getMessage());
        assertSame(branch, error.getNode());
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

    private Node loopCondition(String keyword) {
        return node("COND", node(keyword));
    }

    private Node prefixLoop(Node condition, Node booleanCondition, Node body) {
        return node("LOOP", condition, booleanCondition, node("do"), node("{"), body, node("}"));
    }

    private Node postfixLoop(Node body, Node condition, Node booleanCondition) {
        return node("LOOP", node("do"), node("{"), body, node("}"),
                condition, booleanCondition);
    }

    private Node loop(boolean prefix, Node condition, Node booleanCondition, Node body) {
        return prefix
                ? prefixLoop(condition, booleanCondition, body)
                : postfixLoop(body, condition, booleanCondition);
    }

    private Node noOpInstruction() {
        return node("INSTR", node("nop"));
    }

    private Node algo(Node... instructions) {
        Node rest = node("ALGO");
        for (int i = instructions.length - 1; i >= 0; i--) {
            rest = node("ALGO", instructions[i], node(";"), rest);
        }
        return rest;
    }

    private Node branch(Node condition, Node thenAlgorithm, Node elseAlgorithm) {
        return node("BRANCH",
                node("if"), condition, node("then"), node("{"), thenAlgorithm,
                node("}"), node("else"), node("{"), elseAlgorithm, node("}"));
    }

    private Node output(Node expression) {
        return node("OUTP", node("("), expression, node(")"));
    }
}
