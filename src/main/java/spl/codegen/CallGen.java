package spl.codegen;

import spl.tree.Node;

public class CallGen {

    /** TERM -> CALL: the callee's return value ends up in resultVar. */
    public static String transCallTerm(Node call, String resultVar, CodeGenContext ctx) {
        return inline(call, resultVar, ctx);
    }

    /** INSTR -> CALL: no continuation variable (resultVar == null). */
    public static String transCallInstr(Node call, CodeGenContext ctx) {
        return inline(call, null, ctx);
    }

   
    private static String inline(Node call, String resultVar, CodeGenContext ctx) {

        throw new UnsupportedOperationException("TODO (3b, Person 2): CallGen.inline");
    }
}