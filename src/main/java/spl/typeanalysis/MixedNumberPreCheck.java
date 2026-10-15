package spl.typeanalysis;

import spl.tree.Node;

public class MixedNumberPreCheck {
    public static final String CONFLICT_MESSAGE = "A float-integer-conflict might perhaps be possible";
    public static final String DECIMAL_WITH_MOD_MESSAGE = "A NUM with a decimal dot is not allowed in a program that uses mod";

    private final TypeContext ctx;

    private boolean hasMod;
    private boolean hasDiv;
    private Node firstDecimalNum;
 
    public MixedNumberPreCheck(TypeContext ctx) {
        this.ctx = ctx;
    }
 
    public void run(Node root) {
        hasMod = false;
        hasDiv = false;
        firstDecimalNum = null;
 
        scan(root);
 
        if (hasMod && hasDiv) {
            throw ctx.error(root, CONFLICT_MESSAGE);
        }
        if (hasMod && firstDecimalNum != null) {
            throw ctx.error(firstDecimalNum, DECIMAL_WITH_MOD_MESSAGE);
        }
    }
 
    private void scan(Node n) {
        if (ctx.isOperator(n, "mod")) {
            hasMod = true;
        } else if (ctx.isOperator(n, "div")) {
            hasDiv = true;
        } else if ("NUM".equals(ctx.kind(n)) && firstDecimalNum == null) {
            String t = ctx.text(n);
            if (t != null && t.contains(".")) {
                firstDecimalNum = n;
            }
        }
        for (Node child : ctx.children(n)) {
            scan(child);
        }
    }
}
