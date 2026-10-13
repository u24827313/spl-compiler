package spl.typeanalysis;

import spl.tree.Node;
/**
 * Owner: Person 1.  Rules: SPL_PROG, P, V_DECL, F_DECL, F_TYPE, ALGO.
 * This is the only checker that WRITES name types (ctx.setNameType).
 *
 *  SPL_PROG -> P $                       P ok  => SPL_PROG ok
 *  P        -> V_DECL : F_DECL : ALGO    all three ok => P ok
 *  V_DECL   -> eps                       ok
 *  V_DECL   -> NAME V_DECL               rest ok => NAME numeric, V_DECL ok
 *  F_DECL   -> eps                       ok
 *  F_DECL   -> F_TYPE F_DECL             both ok => ok
 *  F_TYPE   -> void NAME ( V_DECL ) { P return }
 *                                        P ok, V_DECL ok => NAME procedure, F_TYPE ok
 *  F_TYPE   -> num NAME ( V_DECL ) { P return ( TERM ) }
 *                                        P ok, V_DECL ok, TERM numeric => NAME numeric, F_TYPE ok
 *  ALGO     -> eps                       ok
 *  ALGO     -> INSTR ; ALGO              both ok => ok
 *
 * PROPOSED (confirm with team): set the function NAME's type from its header
 * (void -> procedure, num -> numeric) BEFORE checking the body, so recursive calls
 * do not hit "unknown".
 */
public class ProgramTypeChecker {
    private final TypeContext ctx;
 
    public ProgramTypeChecker(TypeContext ctx) {
        this.ctx = ctx;
    }
 
    public Type check(Node n) {
        switch (ctx.kind(n)) {
            case "SPL_PROG": return checkSplProg(n);
            case "P":        return checkP(n);
            case "V_DECL":   return checkVDecl(n);
            case "F_DECL":   return checkFDecl(n);
            case "F_TYPE":   return checkFType(n);
            case "ALGO":     return checkAlgo(n);
            default:
                throw ctx.error(n, "ProgramTypeChecker cannot handle " + ctx.kind(n));
        }
    }
 
    private Type checkSplProg(Node n) { throw new UnsupportedOperationException("TODO"); }
    private Type checkP(Node n)       { throw new UnsupportedOperationException("TODO"); }
    private Type checkVDecl(Node n)   { throw new UnsupportedOperationException("TODO"); }
    private Type checkFDecl(Node n)   { throw new UnsupportedOperationException("TODO"); }
    private Type checkFType(Node n)   { throw new UnsupportedOperationException("TODO"); }
    private Type checkAlgo(Node n)    { throw new UnsupportedOperationException("TODO"); }
}
