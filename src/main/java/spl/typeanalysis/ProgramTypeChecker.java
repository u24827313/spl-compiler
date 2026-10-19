package spl.typeanalysis;

import spl.tree.Node; // TODO: adjust to the real package of Node

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
 * Child layout in the tree (keywords and punctuation are leaf children, in order):
 *  SPL_PROG: [P, $]            P: [V_DECL, :, F_DECL, :, ALGO]
 *  V_DECL:   [] | [NAME, V_DECL]       F_DECL: [] | [F_TYPE, F_DECL]
 *  ALGO:     [] | [INSTR, ;, ALGO]
 *  F_TYPE (void): [void, NAME, (, V_DECL, ), {, P, return, }]                 (9 children)
 *  F_TYPE (num):  [num, NAME, (, V_DECL, ), {, P, return, (, TERM, ), }]      (12 children)
 *  The NAME leaves in V_DECL and F_TYPE are DECLARATIONS: do not ctx.check() them,
 *  set their type with ctx.setNameType(ctx.text(nameLeaf), ...). Empty V_DECL/F_DECL/ALGO
 *  nodes have no children and are simply OK.
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

    private Type checkSplProg(Node n) { 
        checkOk(child(n, 0, 2), "SPL_PROG");
        return Type.OK;
    }
    private Type checkP(Node n)       { 
        checkOk(child(n, 0, 5), "P");
        predeclareFunctions(child(n, 2, 5));
        checkOk(child(n, 2, 5), "P");
        checkOk(child(n, 4, 5), "P");
        return Type.OK;
    }


    private Type checkVDecl(Node n)   { 
        if(ctx.children(n).isEmpty()){
            return Type.OK;
        }

        Node name = child(n, 0, 2);
        checkOk(child(n, 1, 2), "V_DECL");
        declare(name, Type.NUMERIC);
        return Type.OK;
    }

    private Type checkFDecl(Node n)   { 
        if(ctx.children(n).isEmpty()){
            return Type.OK;
        }

        checkOk(child(n, 0, 2), "F_DECL");
        checkOk(child(n, 1, 2), "F_DECL");
        return Type.OK;
    }
    private Type checkFType(Node n)   { 
        if(ctx.children(n).isEmpty()){
            throw ctx.error(n, "Malformed F_TYPE node: no children");
        }

        String header = ctx.kind(ctx.children(n).get(0));
        boolean isNum = header.equals("num");
        if(!isNum && !header.equals("void")){
            throw ctx.error(n, "F_TYPE must start with void or num but starts with '" + header + "'");
        }
        int expected = isNum ? 12 : 9;
        Node name = child(n, 1, expected);
        declare(name, isNum ? Type.NUMERIC : Type.PROCEDURE);

        checkOk(child(n, 3, expected), "F_TYPE");
        checkOk(child(n, 6, expected), "F_TYPE");
        if(isNum){
            Node term = child(n, 9, expected);         // return ( TERM )
            ctx.require(term, ctx.check(term), Type.NUMERIC, "F_TYPE (return value)");
        }

        return Type.OK;
    }
    private Type checkAlgo(Node n)    { 
        if(ctx.children(n).isEmpty()){
            return Type.OK;
        }

        checkOk(child(n, 0, 3), "ALGO");
        checkOk(child(n, 2, 3), "ALGO");
        return Type.OK;
    }

    private Node child(Node n, int i, int expectedCount) {
        if (ctx.children(n).size() != expectedCount) {
            throw ctx.error(n, "Malformed " + ctx.kind(n) + " node: expected " + expectedCount
                    + " children but found " + ctx.children(n).size());
        }
        return ctx.children(n).get(i);
    }

    private void checkOk(Node child, String rule) {
        ctx.require(child, ctx.check(child), Type.OK, rule);
    }

    private void declare(Node nameLeaf, Type t) {
        if (!"NAME".equals(ctx.kind(nameLeaf))) {
            throw ctx.error(nameLeaf, "Expected a name in a declaration but found '"
                    + ctx.text(nameLeaf) + "'");
        }
        ctx.setNameType(nameLeaf, t);
        ctx.setType(nameLeaf, t);
    }

    private void predeclareFunctions(Node fDecl) {
        var children = ctx.children(fDecl);
        if (children.isEmpty()) {
            return;
        }
        if (children.size() != 2) {
            throw ctx.error(fDecl, "Malformed F_DECL node: expected 2 children but found "
                    + children.size());
        }

        Node fType = children.get(0);
        var functionChildren = ctx.children(fType);
        if (functionChildren.isEmpty()) {
            throw ctx.error(fType, "Malformed F_TYPE node: no children");
        }
        String header = ctx.kind(functionChildren.get(0));
        boolean isNum = header.equals("num");
        if (!isNum && !header.equals("void")) {
            throw ctx.error(fType, "F_TYPE must start with void or num but starts with '" + header + "'");
        }
        int expected = isNum ? 12 : 9;
        declare(child(fType, 1, expected), isNum ? Type.NUMERIC : Type.PROCEDURE);
        predeclareFunctions(children.get(1));
    }
}