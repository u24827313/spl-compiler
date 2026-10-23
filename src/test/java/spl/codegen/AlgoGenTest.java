package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.lexer.Lexer;
import spl.lexer.Token;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolTable;
import spl.tree.Node;
import spl.tree.SyntaxTree;

import java.util.List;
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

        Node epsilonAlgo = node("ALGO");
        String epsilonCode = AlgoGen.transAlgo(epsilonAlgo, context);
        assertEquals("/* end */\n", epsilonCode);
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
        SyntaxTree tree = parse(": : nop ; nop ; $ ");
        Node root = tree.getRoot();
        Node mainAlgo = find(root, "ALGO");
        Node nestedAlgo = mainAlgo.getChildren().get(2);
        CodeGenContext context = context();

        String generated = AlgoGen.transAlgo(nestedAlgo, context);
        assertEquals("/* nop */\n/* end */\n", generated);
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
        Node nopToken = node("nop");
        Node nopInstruction = node("INSTR", nopToken);
        Node commentToken = node("comment");
        Node commentText = node("\"text\"");
        Node commentInstruction = node("INSTR", commentToken, commentText);
        Node epsilonTail = node("ALGO");
        Node commentTail = node("ALGO", commentInstruction, node(";"), epsilonTail);
        Node algo = node("ALGO", nopInstruction, node(";"), commentTail);

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
    void translatesStringAndTermPrintsThroughOutp() {
        CodeGenContext context = context();
        SyntaxTree tree = parse(": : print \"hello.world\" ; print ( 7 ) ; $ ");
        Node root = tree.getRoot();
        Node algo = find(root, "ALGO");

        String generated = AlgoGen.transAlgo(algo, context);

        assertEquals("std::cout << \"hello.world\";\n"
                        + "t1 = 7;\nstd::cout << t1;\n/* end */\n",
                generated);
        assertEquals("int t1;\n", context.declarations());
    }

    @Test
    void translatesAssignmentsUsingResolvedSymbolIdentityAndRhsFirst() {
        SyntaxTree tree = parse("#x : : #x = add ( #x 1 ) ; $ ");
        SemanticAnalyser semanticAnalyser = new SemanticAnalyser();
        SymbolTable symbols = semanticAnalyser.analyse(tree);
        CodeGenContext context = new CodeGenContext(symbols, "int");
        Node algo = find(tree.getRoot(), "ALGO");

        String generated = AlgoGen.transAlgo(algo, context);

        assertEquals("t2 = v1;\nt3 = 1;\nt1 = t2 + t3;\nv1 = t1;\n/* end */\n",
                generated);
        assertEquals("int t1;\nint t2;\nint t3;\n", context.declarations());
    }

    @Test
    void rejectsMalformedPrintAndAssignmentLayouts() {
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("print"), node("OUTP")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("print"),
                        node("OUTP", node("("), node("TERM", node("5")))), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("ASSIGN")), context()));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(node("INSTR", node("ASSIGN",
                        node("NAME"), node("="), node("TERM", node("1")))), context()));
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
        SyntaxTree tree = parse(source);
        Node root = tree.getRoot();
        Node mainAlgo = find(root, "ALGO");
        CodeGenContext context = context();
        return AlgoGen.transAlgo(mainAlgo, context);
    }

    private static SyntaxTree parse(String source) {
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(source);
        Parser parser = new Parser();
        return parser.parse(tokens);
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
        int nodeId = IDS.incrementAndGet();
        return node(nodeId, contents, children);
    }

    private static Node node(int nodeId, String contents, Node... children) {
        Node result = new Node(nodeId, contents);
        for (Node child : children) {
            result.addChild(child);
        }
        return result;
    }

    private static CodeGenContext context() {
        return new CodeGenContext(null, "int");
    }
}
