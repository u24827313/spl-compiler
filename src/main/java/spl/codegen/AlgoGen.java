package spl.codegen;

import spl.tree.Node;

/**
 * Person C: translation of ALGO, INSTR, ASSIGN, BRANCH and LOOP.
 *
 * transAlgo works on ANY ALGO subtree (not just the main program), because
 * 3b will reuse it for inlined function bodies.
 *
 * Depends on BoolGen.transBool and TermGen.transTerm (stub while A and B work).
 */
public class AlgoGen {

    /** ALGO -> eps | INSTR ; ALGO */
    public static String transAlgo(Node algo, CodeGenContext ctx) {
        // TODO: if ALGO is empty -> "/* end */\n"
        //       else transInstr(first) + ";\n" + transAlgo(rest)
        throw new UnsupportedOperationException("TODO");
    }

    /** INSTR -> print | nop | comment | ASSIGN | BRANCH | LOOP | CALL(3b) */
    public static String transInstr(Node instr, CodeGenContext ctx) {
        // TODO: dispatch on the first child:
        //   print STRING   -> std::cout << " STRING "
        //   print ( TERM ) -> v = ctx.newVar(); code = transTerm(term, v);
        //                     code + std::cout << v
        //   nop            -> /* nop */
        //   comment STRING -> /* STRING */
        //   ASSIGN / BRANCH / LOOP -> delegate
        //   CALL           -> 3b, throw UnsupportedOperationException("3b")
        throw new UnsupportedOperationException("TODO");
    }

    /** ASSIGN -> NAME = TERM */
    private static String transAssign(Node assign, CodeGenContext ctx) {
        // TODO: v = ctx.newVar(); code = transTerm(term, v);
        //       return code + intern + " = " + v + ";\n"
        throw new UnsupportedOperationException("TODO");
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
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * LOOP: until, while, do-until, do-while.
     *  until BOOL do { ALGO }    : ls: decision(BOOL, le, d) d: body goto ls; le:
     *  while BOOL do { ALGO }    : until not(BOOL) do { ALGO }
     *  do { ALGO } while BOOL    : ld: body decision(BOOL, ld, le) le:
     *  do { ALGO } until BOOL    : do { ALGO } while not(BOOL)
     *
     * Hint: for the "not" cases do not build a new tree; swap the two label
     * arguments instead (same effect as transBool on not(BOOL)).
     */
    private static String transLoop(Node loop, CodeGenContext ctx) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}