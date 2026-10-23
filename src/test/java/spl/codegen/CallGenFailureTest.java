package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.tree.Node;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallGenFailureTest {

    @Test
    void rejectsMalformedCallAndInputShapesBeforeInlining() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": void #show ( ) { : : return } : #show ( ) ; $ ");

        IllegalArgumentException wrongRoot = assertThrows(IllegalArgumentException.class,
                () -> CallGen.transCallInstr(new Node(880_001, "TERM"), program.context()));
        assertTrue(wrongRoot.getMessage().contains("CALL"));

        Node call = CallGenTestSupport.findCallByName(program.root(), "#show");
        call.getChildren().remove(2);
        IllegalArgumentException missingInput = assertThrows(IllegalArgumentException.class,
                () -> CallGen.transCallInstr(call, program.context()));
        assertTrue(missingInput.getMessage().contains("NAME '(' INPUT ')'"));
    }

    @Test
    void rejectsMalformedInputLinkWithNodeContext() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": void #show ( #value ) { : : print ( #value ) ; return } "
                        + ": #show ( 3 ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#show");
        Node input = CallGenTestSupport.directChild(call, "INPUT");
        input.getChildren().get(1).addChild(new Node(880_003, "broken"));

        IllegalArgumentException malformedInput = assertThrows(IllegalArgumentException.class,
                () -> CallGen.transCallInstr(call, program.context()));

        assertTrue(malformedInput.getMessage().contains("INPUT"));
    }

    @Test
    void failedArgumentTranslationRestoresInlineDepthIncludingOverflowAttempt() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": void #show ( #value ) { : : return } : #show ( 3 ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#show");
        Node input = CallGenTestSupport.directChild(call, "INPUT");
        Node argumentTerm = input.getChildren().get(0);
        argumentTerm.getChildren().clear();
        argumentTerm.addChild(new Node(880_002, "pow"));

        for (int level = 0; level < 99; level++) {
            program.context().beginInline();
        }
        assertThrows(IllegalArgumentException.class,
                () -> CallGen.transCallInstr(call, program.context()));

        argumentTerm.getChildren().clear();
        argumentTerm.addChild(new Node(880_004, "7"));
        CallGen.transCallInstr(call, program.context());

        program.context().beginInline();
        assertThrows(IllegalStateException.class, program.context()::beginInline);
        program.context().endInline();
        for (int level = 0; level < 100; level++) {
            program.context().endInline();
        }
    }

    @Test
    void repeatedSequentialCallsDoNotAccumulateInlineDepth() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": void #empty ( ) { : : return } : #empty ( ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#empty");

        for (int invocation = 0; invocation < 250; invocation++) {
            CallGen.transCallInstr(call, program.context());
        }

        for (int level = 0; level < 100; level++) {
            program.context().beginInline();
        }
        assertThrows(IllegalStateException.class, program.context()::beginInline);
        program.context().endInline();
        for (int level = 0; level < 100; level++) {
            program.context().endInline();
        }
    }
}
