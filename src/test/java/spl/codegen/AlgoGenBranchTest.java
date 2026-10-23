package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.tree.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlgoGenBranchTest {
    private static final Pattern LABEL = Pattern.compile("(?m)^(L\\d+):");
    private static final AtomicInteger NODE_IDS = new AtomicInteger(200_000);

    @Test
    void emitsFalseArmBeforeTrueArmWithAnExplicitContinuation() {
        String source = ": : if eq ( 1 1 ) then { print \"t\" ; } else { print \"f\" ; } ; $ ";
        CodeGenTestSupport.Translation translation = CodeGenTestSupport.translate(source, "int");
        String generatedCode = translation.body();

        assertEquals("t1 = 1;\nt2 = 1;\n"
                        + "if ( t1 == t2 ) goto L1;\n"
                        + "L2: /* else */ ;\n"
                        + "std::cout << \"f\";\n/* end */\n"
                        + "goto L3;\n"
                        + "L1: /* then */ ;\n"
                        + "std::cout << \"t\";\n/* end */\n"
                        + "L3: ;\n/* end */\n",
                generatedCode);
        assertFalse(generatedCode.contains("else {"));
        assertEquals("int t1;\nint t2;\n", translation.context().declarations());
    }

    @Test
    void emptyArmsStillHaveDistinctLabelsAndAContinuation() {
        String source = ": : if eq ( 1 2 ) then { } else { } ; nop ; $ ";
        CodeGenTestSupport.Translation translation = CodeGenTestSupport.translate(source, "int");
        String generatedCode = translation.body();

        assertTrue(generatedCode.contains("L2: /* else */ ;\n/* end */\n"));
        assertTrue(generatedCode.contains("goto L3;\nL1: /* then */ ;\n/* end */\nL3: ;\n"));
        assertTrue(generatedCode.endsWith("/* nop */\n/* end */\n"));
        assertUniqueLabels(generatedCode);
    }

    @Test
    void nestedBranchesAndBooleanOperatorsDoNotReuseLabels() {
        String source = ": : if or ( and ( eq ( 1 1 ) not ( eq ( 1 2 ) ) ) larger ( 3 4 ) ) "
                + "then { if lesser ( 1 2 ) then { nop ; } else { comment \"nested\" ; } ; } "
                + "else { nop ; } ; $ ";
        CodeGenTestSupport.Translation translation = CodeGenTestSupport.translate(source, "int");
        String generatedCode = translation.body();

        assertUniqueLabels(generatedCode);
        assertAllGotoTargetsExist(generatedCode);
        assertTrue(generatedCode.contains("/* nested */"));
    }

    @Test
    void rejectsMalformedBranchTreesInsteadOfIndexingArbitraryChildren() {
        Node branch = node("BRANCH");
        Node instr = node("INSTR", branch);

        CodeGenContext context = new CodeGenContext(null, "int");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(instr, context));
        assertTrue(error.getMessage().contains("BRANCH"));
    }

    private static void assertUniqueLabels(String code) {
        List<String> labelNames = labels(code);
        Set<String> uniqueLabelNames = new HashSet<>(labelNames);
        assertEquals(labelNames.size(), uniqueLabelNames.size(), "duplicate label definitions in:\n" + code);
    }

    private static void assertAllGotoTargetsExist(String code) {
        List<String> labelNames = labels(code);
        Set<String> definedLabelNames = new HashSet<>(labelNames);
        Pattern gotoPattern = Pattern.compile("\\bgoto\\s+(L\\d+)\\s*;");
        Matcher gotos = gotoPattern.matcher(code);
        List<String> missing = new ArrayList<>();
        while (gotos.find()) {
            String targetLabel = gotos.group(1);
            if (!definedLabelNames.contains(targetLabel)) {
                missing.add(targetLabel);
            }
        }
        assertEquals(List.of(), missing, "unresolved goto target in:\n" + code);
    }

    private static List<String> labels(String code) {
        Matcher matcher = LABEL.matcher(code);
        List<String> labels = new ArrayList<>();
        while (matcher.find()) {
            labels.add(matcher.group(1));
        }
        return labels;
    }

    private static Node node(String contents, Node... children) {
        int nodeId = NODE_IDS.incrementAndGet();
        Node node = new Node(nodeId, contents);
        for (Node child : children) {
            node.addChild(child);
        }
        return node;
    }
}
