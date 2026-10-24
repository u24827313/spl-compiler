package spl.typeanalysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.tree.Node;
import spl.tree.SyntaxTree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Parser-to-type-analysis integration tests for Person 3's rules.
 * These deliberately do not run Phase 2a or the Main entry point.
 */
class DonovanTypeAnalysisIntegrationTest {

    @Test
    void acceptsEmptyProgram() {
        assertAccepted(": : $ ");
    }

    @Test
    void acceptsBasicInstructionsAndBothOutputForms() {
        String source = "#x : : nop ; comment \"hello.world\" ; print \"plain\" ; "
                + "print ( 5 ) ; #x = add ( #x 1 ) ; $ ";

        ParsedProgram program = parse(source);

        assertEquals(Type.OK, program.analyse());
        assertEquals(Type.OK, program.context().typeOf(program.root()));
        assertEquals(Type.NUMERIC, program.context().nameType("#x"));
    }

    @Test
    void acceptsBranchThroughParser() {
        ParsedProgram program = parse(
                ": : if eq ( 1 1 ) then { nop ; } else { nop ; } ; $ ");

        assertEquals(Type.OK, program.analyse());
        assertEquals(Type.OK, program.context().typeOf(findFirst(program.root(), "BRANCH")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ": : while lesser ( 1 2 ) do { nop ; } ; $ ",
            ": : until eq ( 1 2 ) do { nop ; } ; $ ",
            ": : do { nop ; } while lesser ( 1 2 ) ; $ ",
            ": : do { nop ; } until eq ( 1 1 ) ; $ "
    })
    void acceptsAllLoopLayoutsAndConditionKeywords(String source) {
        ParsedProgram program = parse(source);

        assertEquals(Type.OK, program.analyse());
        assertEquals(Type.OK, program.context().typeOf(findFirst(program.root(), "LOOP")));
    }

    @Test
    void acceptsProcedureCallAsInstruction() {
        ParsedProgram program = parse(
                ": void #p ( ) { : : return } : #p ( ) ; $ ");

        assertEquals(Type.OK, program.analyse());
        assertEquals(Type.PROCEDURE, program.context().typeOf(findFirst(program.root(), "CALL")));
        assertEquals(Type.OK, program.context().typeOf(findFirst(program.root(), "INSTR")));
    }

    @Test
    void acceptsNumericFunctionCallInsideOutputExpression() {
        ParsedProgram program = parse(
                ": num #f ( ) { : : return ( 1 ) } : print ( #f ( ) ) ; $ ");

        assertEquals(Type.OK, program.analyse());
        assertEquals(Type.NUMERIC, program.context().typeOf(findFirst(program.root(), "CALL")));
        assertEquals(Type.OK, program.context().typeOf(findFirst(program.root(), "OUTP")));
    }

    @Test
    void acceptsNestedBranchAndLoopFormsThroughParser() {
        String source = ": : if eq ( 1 1 ) then { while eq ( 1 1 ) do { nop ; } ; } "
                + "else { do { nop ; } until eq ( 1 1 ) ; } ; $ ";
        ParsedProgram program = parse(source);

        assertEquals(Type.OK, program.analyse());
        assertEquals(Type.OK, program.context().typeOf(findFirst(program.root(), "BRANCH")));
        assertEquals(Type.OK, program.context().typeOf(findFirst(program.root(), "LOOP")));
    }

    @Test
    void rejectsNumericFunctionCallUsedAsInstruction() {
        ParsedProgram program = parse(
                ": num #f ( ) { : : return ( 1 ) } : #f ( ) ; $ ");

        TypeException error = assertThrows(TypeException.class, program::analyse);

        assertEquals("Expected a procedure for a function call used as an instruction, but found a numeric value.",
                error.getMessage());
        assertEquals("CALL", error.getNode().getContents());
    }

    @Test
    void rejectsProcedureCallInsideNumericOutputTerm() {
        ParsedProgram program = parse(
                ": void #p ( ) { : : return } : print ( #p ( ) ) ; $ ");

        TypeException error = assertThrows(TypeException.class, program::analyse);

        assertEquals("Expected a numeric value for a numeric expression, but found a procedure.",
                error.getMessage());
        assertEquals("CALL", error.getNode().getContents());
    }

    @Test
    void rejectsModAndDivAcrossInstructions() {
        ParsedProgram program = parse(
                ": : print ( mod ( 5 2 ) ) ; print ( div ( 8 2 ) ) ; $ ");

        TypeException error = assertThrows(TypeException.class, program::analyse);

        assertEquals("Incompatible numeric operations", error.getDescription());
        assertEquals(MixedNumberPreCheck.CONFLICT_MESSAGE, error.getMessage());
        assertSame(program.root(), error.getNode());
    }

    @Test
    void rejectsDecimalNumWhenModAppearsElsewhere() {
        ParsedProgram program = parse(
                ": : print ( mod ( 5 2 ) ) ; print ( 1.5 ) ; $ ");

        TypeException error = assertThrows(TypeException.class, program::analyse);

        assertEquals("Invalid modulo value", error.getDescription());
        assertEquals(MixedNumberPreCheck.DECIMAL_WITH_MOD_MESSAGE, error.getMessage());
        assertEquals("1.5", error.getNode().getContents());
    }

    @Test
    void mixedNumberPrecheckRunsBeforeOrdinaryTypeCrawl() {
        ParsedProgram program = parse(": num #f ( ) { : : return ( 1 ) } : #f ( ) ; "
                + "print ( mod ( 5 2 ) ) ; print ( div ( 8 2 ) ) ; $ ");

        TypeException error = assertThrows(TypeException.class, program::analyse);

        assertEquals(MixedNumberPreCheck.CONFLICT_MESSAGE, error.getMessage());
        assertSame(program.root(), error.getNode());
        assertEquals(Type.UNKNOWN, program.context().typeOf(program.root()));
        assertEquals(Type.UNKNOWN, program.context().typeOf(findFirst(program.root(), "P")));
        assertEquals(Type.UNKNOWN, program.context().typeOf(findFirst(program.root(), "INSTR")));
        assertEquals(Type.UNKNOWN, program.context().nameType("#f"));
    }

    private void assertAccepted(String source) {
        ParsedProgram program = parse(source);

        assertEquals(Type.OK, program.analyse());
        assertEquals(Type.OK, program.context().typeOf(program.root()));
    }

    private ParsedProgram parse(String source) {
        SyntaxTree tree = new Parser().parse(new Lexer().tokenize(source));
        return new ParsedProgram(tree.getRoot(), new TypeContext());
    }

    private Node findFirst(Node node, String contents) {
        if (contents.equals(node.getContents())) {
            return node;
        }

        for (Node child : node.getChildren()) {
            Node match = findFirst(child, contents);
            if (match != null) {
                return match;
            }
        }

        return null;
    }

    private record ParsedProgram(Node root, TypeContext context) {
        private Type analyse() {
            return new TypeAnalyser(context).analyse(root);
        }
    }
}
