package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.tree.Node;
import spl.tree.SyntaxTree;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlgoGenTest {
    private static final AtomicInteger IDS = new AtomicInteger(100_000);

    @Test
    void translatesEpsilonAlgoWithoutAllocatingTemporariesOrLabels() {
        CodeGenContext context = context();

        assertEquals("/* end */\n", AlgoGen.transAlgo(node("ALGO"), context));
        assertEquals("/* end */\n", translate(": : $ "));
        assertEquals("", context.declarations());
        assertEquals("t1", context.newVar());
        assertEquals("L1", context.newLabel());
    }

    @Test
    void translatesSingleAndSequentialNopInstructions() {
        assertEquals("/* nop */\n/* end */\n",
                translate(": : nop ; $ "));
        assertEquals("/* nop */\n/* nop */\n/* end */\n",
                translate(": : nop ; nop ; $ "));
    }

    @Test
    void translatesCommentsWithoutQuotesOrOutputInstructions() {
        String generated = translate(": : comment \"hello.world\" ; nop ; $ ");

        assertEquals("/* hello.world */\n/* nop */\n/* end */\n", generated);
        assertFalse(generated.contains("std::cout"));
    }

    @Test
    void canTranslateAParsedNestedAlgoSubtreeDirectly() {
        Node mainAlgo = find(parse(": : nop ; nop ; $ ").getRoot(), "ALGO");
        Node nestedAlgo = mainAlgo.getChildren().get(2);

        assertEquals("/* nop */\n/* end */\n", AlgoGen.transAlgo(nestedAlgo, context()));
    }

    @Test
    void preservesInstructionOrderAcrossLongerChains() {
        StringBuilder source = new StringBuilder(": : ");
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            source.append("comment \"item").append(i).append("\" ; ");
            expected.append("/* item").append(i).append(" */\n");
        }
        source.append("$ ");
        expected.append("/* end */\n");

        assertEquals(expected.toString(), translate(source.toString()));
    }

    @Test
    void nopAndCommentDoNotConsumeTemporaryOrLabelNames() {
        CodeGenContext context = context();
        Node algo = node("ALGO", node("INSTR", node("nop")), node(";"), node("ALGO",
                node("INSTR", node("comment"), node("\"text\"")), node(";"), node("ALGO")));

        AlgoGen.transAlgo(algo, context);

        assertEquals("", context.declarations());
        assertEquals("t1", context.newVar());
        assertEquals("L1", context.newLabel());
    }

    @Test
    void rejectsNonAlgoRootsAndMalformedAlgoLayouts() {
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transAlgo(node("INSTR", node("nop")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transAlgo(node("ALGO", node("INSTR", node("nop"))), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transAlgo(node("ALGO", node("INSTR", node("nop")), node(":"), node("ALGO")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transAlgo(node("ALGO", node("INSTR", node("nop")), node(";")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transAlgo(node("ALGO", node("INSTR", node("nop")), node(";"), node("ALGO"), node("extra")), context()));
    }

    @Test
    void rejectsMalformedOrUnknownInstructionAlternatives() {
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR"), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("mystery")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("nop"), node("extra")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("comment")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("comment"), node("not-a-string")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("nop", node("nested"))), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("print")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("print"), node("not-OUTP")), context()));
    }

    @Test
    void preventsSyntheticCommentTextFromBreakingGeneratedCode() {
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("comment"), node("\"bad */ text\"")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("comment"), node("\"bad\ntext\"")), context()));
    }

    @Test
    void leavesLaterPhaseInstructionsExplicitlyUnimplemented() {
        UnsupportedOperationException print = assertThrows(UnsupportedOperationException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("print"), node("OUTP")), context()));
        assertTrue(print.getMessage().contains("Phase 2"));

        UnsupportedOperationException assign = assertThrows(UnsupportedOperationException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("ASSIGN")), context()));
        assertTrue(assign.getMessage().contains("Phase 2"));

        UnsupportedOperationException branch = assertThrows(UnsupportedOperationException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("BRANCH")), context()));
        assertTrue(branch.getMessage().contains("Phase 3"));

        UnsupportedOperationException loop = assertThrows(UnsupportedOperationException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("LOOP")), context()));
        assertTrue(loop.getMessage().contains("Phase 4"));
    }

    @Test
    void reportsStatementCallsAsDeferredToPhase3b() {
        UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("CALL")), context()));
        assertTrue(error.getMessage().contains("3b"));

        Node algo = node("ALGO", node("INSTR", node("CALL")), node(";"), node("ALGO"));
        assertThrows(UnsupportedOperationException.class, () -> AlgoGen.transAlgo(algo, context()));
    }

    private static String translate(String source) {
        Node mainAlgo = find(parse(source).getRoot(), "ALGO");
        return AlgoGen.transAlgo(mainAlgo, context());
    }

    private static SyntaxTree parse(String source) {
        return new Parser().parse(new Lexer().tokenize(source));
    }

    private static Node find(Node root, String contents) {
        Node found = findOrNull(root, contents);
        if (found == null) {
            throw new AssertionError("Missing node " + contents);
        }
        return found;
    }

    private static Node findOrNull(Node root, String contents) {
        if (contents.equals(root.getContents())) {
            return root;
        }
        for (Node child : root.getChildren()) {
            Node found = findOrNull(child, contents);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Node node(String contents, Node... children) {
        return node(IDS.incrementAndGet(), contents, children);
    }

    private static Node node(int id, String contents, Node... children) {
        Node result = new Node(id, contents);
        for (Node child : children) {
            result.addChild(child);
        }
        return result;
    }

    private static CodeGenContext context() {
        return new CodeGenContext(null, "int");
    }
}
