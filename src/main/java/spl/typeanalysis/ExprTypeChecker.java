package spl.typeanalysis;

import spl.tree.Node;

public class ExprTypeChecker {
    private final TypeContext ctx;
 
    public ExprTypeChecker(TypeContext ctx) {
        this.ctx = ctx;
    }
 
    public Type check(Node n) {
        switch (ctx.kind(n)) {
            case "TERM":  return checkTerm(n);
            case "BOOL":  return checkBool(n);
            case "CALL":  return checkCall(n);
            case "INPUT": return checkInput(n);
            default:
                throw ctx.error(n, "ExprTypeChecker cannot handle " + ctx.kind(n));
        }
    }
 
    private Type checkTerm(Node n)  { throw new UnsupportedOperationException("TODO"); }
    private Type checkBool(Node n)  { throw new UnsupportedOperationException("TODO"); }
    private Type checkCall(Node n)  { throw new UnsupportedOperationException("TODO"); }
    private Type checkInput(Node n) { throw new UnsupportedOperationException("TODO"); }
}
