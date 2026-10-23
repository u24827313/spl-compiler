package spl.codegen;

import java.util.List;
import spl.tree.Node;

import static spl.codegen.GenKinds.*;

/**
 * Person C: translation of ALGO, INSTR, ASSIGN, BRANCH and LOOP.
 *
 * transAlgo works on ANY ALGO subtree (not just the main program), because
 * 3b will reuse it for inlined function bodies.
 */
public class AlgoGen {

    /** ALGO -> eps | INSTR ; ALGO */
    public static String transAlgo(Node algo, CodeGenContext ctx) {
        requireKind(algo, ALGO);
        List<Node> children = algo.getChildren();
        if (children.isEmpty()) {
            return "/* end */\n";
        }
        if (children.size() != 3 || !is(children.get(0), INSTR)
                || !is(children.get(1), ";") || !is(children.get(2), ALGO)) {
            throw malformed(algo, "expected INSTR, ';', ALGO or epsilon");
        }

        return transInstr(children.get(0), ctx) + transAlgo(children.get(2), ctx);
    }

    /** INSTR -> print | nop | comment | ASSIGN | BRANCH | LOOP | CALL(3b) */
    public static String transInstr(Node instr, CodeGenContext ctx) {
        requireKind(instr, INSTR);
        List<Node> children = instr.getChildren();
        if (children.isEmpty()) {
            throw malformed(instr, "instruction has no alternative");
        }

        Node first = children.get(0);
        String alternative = first.getContents();
        switch (alternative) {
            case "nop":
                requireLeafToken(first, "nop", instr);
                requireChildCount(instr, 1, "nop instruction takes no operands");
                return "/* nop */\n";
            case "comment":
                requireLeafToken(first, "comment", instr);
                requireChildCount(instr, 2, "comment instruction requires one STRING operand");
                Node string = children.get(1);
                String text = quotedString(string, instr);
                if (text.contains("*/") || text.contains("\n") || text.contains("\r")) {
                    throw malformed(instr, "comment text cannot contain a comment terminator or line break");
                }
                return "/* " + stripQuotes(text) + " */\n";
            case "print":
                requireLeafToken(first, "print", instr);
                requireChildCount(instr, 2, "print instruction requires one OUTP child");
                if (!is(children.get(1), OUTP)) {
                    throw malformed(instr, "expected OUTP after print");
                }
                throw new UnsupportedOperationException("Phase 2: print translation is not implemented");
            case ASSIGN:
                requireChildCount(instr, 1, "ASSIGN must be the only instruction child");
                return transAssign(first, ctx);
            case BRANCH:
                requireChildCount(instr, 1, "BRANCH must be the only instruction child");
                return transBranch(first, ctx);
            case LOOP:
                requireChildCount(instr, 1, "LOOP must be the only instruction child");
                return transLoop(first, ctx);
            case CALL:
                requireChildCount(instr, 1, "CALL must be the only instruction child");
                throw new UnsupportedOperationException("3b: function call as instruction");
            default:
                throw new IllegalArgumentException("Unknown INSTR alternative '" + alternative
                        + "' at node " + instr.getId());
        }
    }

    private static void requireKind(Node node, String expected) {
        if (node == null || !is(node, expected)) {
            String found = node == null ? "null" : "'" + node.getContents() + "'";
            String id = node == null ? "" : " at node " + node.getId();
            throw new IllegalArgumentException("Expected " + expected + " node but found " + found + id);
        }
    }

    private static void requireChildCount(Node node, int expected, String detail) {
        if (node.getChildren().size() != expected) {
            throw malformed(node, detail + "; found " + node.getChildren().size() + " children");
        }
    }

    private static void requireLeafToken(Node token, String expected, Node parent) {
        if (!token.isLeaf() || !expected.equals(token.getContents())) {
            throw malformed(parent, "expected '" + expected + "' token leaf");
        }
    }

    private static String quotedString(Node string, Node parent) {
        String text = string.getContents();
        if (!string.isLeaf() || text.length() < 2 || !text.startsWith("\"") || !text.endsWith("\"")) {
            throw malformed(parent, "expected a quoted STRING operand");
        }
        return text;
    }

    private static IllegalArgumentException malformed(Node node, String detail) {
        return new IllegalArgumentException("Malformed " + node.getContents() + " node "
                + node.getId() + ": " + detail);
    }

    /** ASSIGN -> NAME = TERM; implemented in Phase 2. */
    private static String transAssign(Node assign, CodeGenContext ctx) {
        throw new UnsupportedOperationException("Phase 2: assignment translation is not implemented");
    }

    /**
     * BRANCH -> if BOOL then { ALGO_true } else { ALGO_false }
     *
     * Layout:   (comment "if")  decisionCode(BOOL, La, Lb)
     *           Lb: (comment "else")  falseCode  goto Lc;
     *           La: (comment "then")  trueCode
     *           Lc: (comment "continue")
     */
    private static String transBranch(Node branch, CodeGenContext ctx) {
        // TODO: La, Lb, Lc = ctx.newLabel() x3
        throw new UnsupportedOperationException("Phase 3: branch translation is not implemented");
    }

    /**
     * LOOP: until, while, do-until, do-while.
     *  until BOOL do { ALGO }    : ls: decision(BOOL, le, d) d: body goto ls; le:
     *  while BOOL do { ALGO }    : until not(BOOL) do { ALGO }
     *  do { ALGO } while BOOL    : ld: body decision(BOOL, ld, le) le:
     *  do { ALGO } until BOOL    : do { ALGO } while not(BOOL)
     *
     * For the negated cases, use BoolGen's semantic negation helper; simply
     * swapping target labels does not negate under the true-jump/fall-through contract.
     */
    private static String transLoop(Node loop, CodeGenContext ctx) {
        // TODO
        throw new UnsupportedOperationException("Phase 4: loop translation is not implemented");
    }
}
