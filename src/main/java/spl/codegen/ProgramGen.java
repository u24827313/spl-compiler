package spl.codegen;

import spl.tree.Node;

public class ProgramGen {

    /** SPL_PROG -> P $ ; returns trans(P) + "return 0;" + closing brace. */
    public static String transSplProg(Node splProg, CodeGenContext ctx) {
  
        throw new UnsupportedOperationException("TODO (3b, Person 3): transSplProg");
    }

    /** P -> V_DECL : F_DECL : ALGO */
    static String transP(Node p, CodeGenContext ctx) {
        // TODO (Person 3): mine = asComment(transFDecl(F_DECL, ctx.scratch()));
        //                  header, main(), body = AlgoGen.transAlgo(ALGO, ctx), ...
        throw new UnsupportedOperationException("TODO (3b, Person 3): transP");
    }

    /** V_DECL -> eps | NAME V_DECL ; "typ name;\n" per name (empty -> one blank space). */
    static String transVDecl(Node vdecl, CodeGenContext ctx) {
        // TODO (Person 3): documentation text for the mine; real declarations are hoisted
        throw new UnsupportedOperationException("TODO (3b, Person 3): transVDecl");
    }

    /** F_DECL -> eps | F_TYPE F_DECL ; commented code-mine text (pass ctx.scratch()). */
    static String transFDecl(Node fDecl, CodeGenContext scratch) {

        throw new UnsupportedOperationException("TODO (3b, Person 3): transFDecl");
    }

    /** F_TYPE -> void/num NAME ( V_DECL ) { P return [( TERM )] } */
    static String transFType(Node fType, CodeGenContext scratch) {

        throw new UnsupportedOperationException("TODO (3b, Person 3): transFType");
    }

    /**
     * Wrap generated code in a C comment. The code already contains comments
     * (/* if *&#47;, /* end *&#47; ...), so inner markers must be neutralised first,
     * e.g. replace "/*" with "/ *" and "*&#47;" with "* /", then wrap.
     */
    static String asComment(String code) {

        throw new UnsupportedOperationException("TODO (3b, Person 3): asComment");
    }

    /**
     * Walk EVERY V_DECL in the tree (main's, and every function's parameters and
     * locals) and call ctx.declareUserVar(ctx.lookUp(nameLeaf)) so each user
     * variable is declared exactly once at the top of main().
     */
    static void declareUserVars(Node splProgRoot, CodeGenContext ctx) {

        throw new UnsupportedOperationException("TODO (3b, Person 3): declareUserVars");
    }

    /** "float" if any NUM leaf contains a '.', otherwise "int". */
    static String detectCType(Node splProgRoot) {

        throw new UnsupportedOperationException("TODO (3b, Person 3): detectCType");
    }
}