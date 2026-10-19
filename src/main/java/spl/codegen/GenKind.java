package spl.codegen;

import spl.tree.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * Grammar-name constants and small tree helpers for the code generator.
 *
 * These mirror spl.semantic.NodeKinds (which is package-private), so 2a does
 * not need to change. ALL assumptions about the shape of the syntax tree live
 * in this file and in the dispatch code of the three translators.
 */
final class GenKinds {
    static final String ALGO = "ALGO", INSTR = "INSTR", OUTP = "OUTP",
            ASSIGN = "ASSIGN", BRANCH = "BRANCH", LOOP = "LOOP", COND = "COND",
            BOOL = "BOOL", TERM = "TERM", CALL = "CALL";

    private GenKinds() {}

    static boolean is(Node n, String contents) {
        return n != null && n.getContents().equals(contents);
    }

    /** Same rule as 2a: user-defined NAME leaves start with '#'. */
    static boolean isUserDefinedName(Node n) {
        return n.isLeaf() && n.getContents().startsWith("#");
    }

    static Node childNamed(Node n, String contents) {
        if (n == null) return null;
        for (Node c : n.getChildren()) if (is(c, contents)) return c;
        return null;
    }

    static List<Node> childrenNamed(Node n, String contents) {
        List<Node> out = new ArrayList<>();
        for (Node c : n.getChildren()) if (is(c, contents)) out.add(c);
        return out;
    }

    /** Text of a leaf, or of the single leaf below a wrapper node. */
    static String leafText(Node n) {
        Node cur = n;
        while (!cur.isLeaf() && cur.getChildren().size() == 1) cur = cur.getChildren().get(0);
        return cur.getContents();
    }

    static String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /** A C label line. The ';' keeps it valid even if nothing follows it. */
    static String label(String name, String comment) {
        return name + ":" + (comment.isEmpty() ? "" : " /* " + comment + " */") + " ;\n";
    }
}