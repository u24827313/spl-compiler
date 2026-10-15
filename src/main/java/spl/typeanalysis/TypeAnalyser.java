package spl.typeanalysis;

import spl.tree.Node;

public class TypeAnalyser {
    private final TypeContext ctx;
 
    public TypeAnalyser() {
        this(new TypeContext());
    }
 
    public TypeAnalyser(TypeContext ctx) {
        this.ctx = ctx;
    }
 
    public TypeContext context() {
        return ctx;
    }
 
    /** @param root the SPL_PROG node produced by the parser (after phase 2a) */
    public Type analyse(Node root) {
        new MixedNumberPreCheck(ctx).run(root);
        Type t = ctx.check(root);
        ctx.require(root, t, Type.OK, "SPL_PROG");
        return t;
    }
}
