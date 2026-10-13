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
    private Type checkOutp(Node n)   { throw new UnsupportedOperationException("TODO"); }
    private Type checkAssign(Node n) { throw new UnsupportedOperationException("TODO"); }
    private Type checkBranch(Node n) { throw new UnsupportedOperationException("TODO"); }
    private Type checkLoop(Node n)   { throw new UnsupportedOperationException("TODO"); }
    private Type checkCond(Node n)   { throw new UnsupportedOperationException("TODO"); }
}