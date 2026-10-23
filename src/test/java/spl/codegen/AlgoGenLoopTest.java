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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlgoGenLoopTest {
    private static final Pattern LABEL = Pattern.compile("(?m)^(L\\d+):");
    private static final AtomicInteger NODE_IDS = new AtomicInteger(300_000);

    @Test
    void translatesAllFourLoopFormsWithTheCorrectDecisionPosition() {
        String prefixUntilSource = ": : until eq ( 1 1 ) do { nop ; } ; $ ";
        CodeGenTestSupport.Translation prefixUntilTranslation =
                CodeGenTestSupport.translate(prefixUntilSource, "int");
        String prefixUntil = prefixUntilTranslation.body();
        assertTrue(prefixUntil.startsWith("L1: /* loop */ ;\n"));
        assertTrue(prefixUntil.contains("if ( t1 == t2 ) goto L2;\nL3: /* do */ ;\n"));
        assertTrue(prefixUntil.contains("goto L1;\nL2: /* exit */ ;\n"));

        String prefixWhileSource = ": : while eq ( 1 1 ) do { nop ; } ; $ ";
        CodeGenTestSupport.Translation prefixWhileTranslation =
                CodeGenTestSupport.translate(prefixWhileSource, "int");
        String prefixWhile = prefixWhileTranslation.body();
        assertTrue(prefixWhile.startsWith("L1: /* loop */ ;\n"));
        assertTrue(prefixWhile.contains("if ( t1 == t2 ) goto L4;\ngoto L2;\nL4: ;\n"));
        assertTrue(prefixWhile.contains("L3: /* do */ ;\n/* nop */\n"));
        assertTrue(prefixWhile.contains("goto L1;\nL2: /* exit */ ;\n"));

        String postfixWhileSource = ": : do { nop ; } while eq ( 1 1 ) ; $ ";
        CodeGenTestSupport.Translation postfixWhileTranslation =
                CodeGenTestSupport.translate(postfixWhileSource, "int");
        String postfixWhile = postfixWhileTranslation.body();
        assertTrue(postfixWhile.startsWith("L1: /* do */ ;\n/* nop */\n"));
        assertTrue(postfixWhile.contains("if ( t1 == t2 ) goto L1;\nL2: /* exit */ ;\n"));

        String postfixUntilSource = ": : do { nop ; } until eq ( 1 1 ) ; $ ";
        CodeGenTestSupport.Translation postfixUntilTranslation =
                CodeGenTestSupport.translate(postfixUntilSource, "int");
        String postfixUntil = postfixUntilTranslation.body();
        assertTrue(postfixUntil.startsWith("L1: /* do */ ;\n/* nop */\n"));
        assertTrue(postfixUntil.contains("if ( t1 == t2 ) goto L3;\n"));
        assertTrue(postfixUntil.contains("goto L1;\nL3: ;\nL2: /* exit */ ;\n"));
    }

    @Test
    void nestedLoopsAndFollowingInstructionsKeepLabelsUniqueAndContinueAfterExit() {
        String source = ": : while eq ( 1 2 ) do { do { nop ; } until not ( eq ( 1 1 ) ) ; } ; nop ; $ ";
        CodeGenTestSupport.Translation translation = CodeGenTestSupport.translate(source, "int");
        String generatedCode = translation.body();
        List<String> labelNames = labels(generatedCode);
        Set<String> uniqueLabelNames = new HashSet<>(labelNames);

        assertEquals(labelNames.size(), uniqueLabelNames.size(), generatedCode);
        assertAllGotoTargetsExist(generatedCode);
        assertTrue(generatedCode.endsWith("/* nop */\n/* end */\n"));
    }

    @Test
    void rejectsMalformedLoopLayoutsAndConditions() {
        Node malformed = node("LOOP");
        Node malformedInstruction = node("INSTR", malformed);
        CodeGenContext context = new CodeGenContext(null, "int");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(malformedInstruction, context));
        assertTrue(error.getMessage().contains("LOOP"));

        Node unexpectedConditionToken = node("repeat");
        Node badCondition = node("COND", unexpectedConditionToken);
        Node badBoolean = node("BOOL", node("eq"));
        Node bodyAlgo = node("ALGO");
        Node badPrefix = node("LOOP", badCondition, badBoolean, node("do"), node("{"),
                bodyAlgo, node("}"));
        Node badLoopInstruction = node("INSTR", badPrefix);
        assertThrows(IllegalArgumentException.class,
                () -> AlgoGen.transInstr(badLoopInstruction, context));
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
