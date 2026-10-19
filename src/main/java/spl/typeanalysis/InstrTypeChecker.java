package spl.typeanalysis;

import spl.tree.Node; // TODO: adjust to the real package of Node

/**
 * Owner: Person 3.  Rules: INSTR, OUTP, ASSIGN, BRANCH, LOOP, COND.
 *
 *  INSTR  -> print OUTP | ASSIGN | BRANCH | LOOP   child ok => INSTR ok
 *  INSTR  -> nop | comment STRING                  ok
 *  INSTR  -> CALL                                  CALL must be "procedure" => INSTR ok
 *                                                  (a numeric function used as a statement is an error)
 *  OUTP   -> ( TERM )                              TERM numeric => OUTP ok
 *  OUTP   -> STRING                                ok
 *  ASSIGN -> NAME = TERM                           NAME numeric and TERM numeric => ASSIGN ok
 *  BRANCH -> if BOOL then { ALGO } else { ALGO }   both ALGO ok, BOOL boolean => BRANCH ok
 *  COND   -> while | until                         ok
 *  LOOP   -> COND BOOL do { ALGO }
 *  LOOP   -> do { ALGO } COND BOOL                 ALGO ok, COND ok, BOOL boolean => LOOP ok
 *
 * Child layout in the tree:
 *  INSTR:  [comment, STRING] | [print, OUTP] | [ASSIGN] | [CALL] | [BRANCH] | [LOOP] | [nop]
 *  OUTP:   [(, TERM, )] | [STRING]
 *  ASSIGN: [NAME, =, TERM]
 *  BRANCH: [if, BOOL, then, {, ALGO, }, else, {, ALGO, }]
 *  LOOP:   [COND, BOOL, do, {, ALGO, }]  (while form)  |  [do, {, ALGO, }, COND, BOOL]  (until form)
 *  COND:   [while] | [until]
 *  The two LOOP forms are told apart by ctx.kind(first child): "COND" or "do".
 *
 * Which INSTR variant a node is: look at its first child (print / nop / comment / ASSIGN ...).
 */
public class InstrTypeChecker {

    private final TypeContext ctx;

    public InstrTypeChecker(TypeContext ctx) {
        this.ctx = ctx;
    }

    public Type check(Node n) {
        switch (ctx.kind(n)) {
            case "INSTR":  return checkInstr(n);
            case "OUTP":   return checkOutp(n);
            case "ASSIGN": return checkAssign(n);
            case "BRANCH": return checkBranch(n);
            case "LOOP":   return checkLoop(n);
            case "COND":   return checkCond(n);
            default:
                throw ctx.error(n, "InstrTypeChecker cannot handle " + ctx.kind(n));
        }
    }

    private Type checkInstr(Node n)  { throw new UnsupportedOperationException("TODO"); }

    private Type checkOutp(Node n) {
        var children = ctx.children(n);


        if (children.size() == 1) {
            Node child = children.get(0) ;
            String kind = ctx.kind(child);
            if(!kind.equals("STRING")){
                throw ctx.error(n, "Malformed OUTP: expected STRING but found "+ kind);
            }

            ctx.require(child, ctx.check(child), Type.OK, "OUTP -> STRING");
            return Type.OK;
        }

        int size = children.size();
        if (size == 3) {
            Node leftChild = children.get(0) ;
            Node rightChild = children.get(2);
            String leftKind = ctx.kind(leftChild);
            String rightKind = ctx.kind(rightChild);

            if(leftKind.equals("(") && rightKind.equals(")")){
                requireChildType(n, 1, Type.NUMERIC, "OUTP -> ( TERM )");
                return Type.OK;
            }

            throw ctx.error(n, "Malformed OUTP: expected ( TERM )");
        }
        else {
            throw ctx.error(n, "Malformed OUTP: expected [STRING] or [(, TERM, )] but found "
                    + children.size() + " children");
        }
    }

    private Type checkAssign(Node n) { throw new UnsupportedOperationException("TODO"); }
    private Type checkBranch(Node n) { throw new UnsupportedOperationException("TODO"); }
    private Type checkLoop(Node n)   { throw new UnsupportedOperationException("TODO"); }

    private Type checkCond(Node n) {
        requireChildCount(n, 1);

        Node keyword = ctx.children(n).get(0);
        String condition = ctx.kind(keyword);

        if (!(condition.equals("while") || condition.equals("until"))){
            throw ctx.error(n, "Malformed COND: expected while or until but found " + condition);
        }

        return Type.OK;
    }

    private void requireChildType(Node parent, int index, Type expected, String rule) {
        Node child = ctx.children(parent).get(index);
        ctx.require(child, ctx.check(child), expected, rule);
    }

    private void requireChildCount(Node n, int expected) {
        int actual = ctx.children(n).size();
        if (actual != expected) {
            throw ctx.error(n, "Malformed " + ctx.kind(n) + ": expected " + expected
                    + " children but found " + actual);
        }
    }
}
