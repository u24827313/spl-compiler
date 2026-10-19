package spl.codegen;

import spl.tree.Node;

/**
 * Person A: translation of TERM (Textbook Fig. 6.3 style).
 *
 * Contract: transTerm writes its result into resultVar and returns the C code
 * as a String. It never returns the value itself.
 */
public class TermGen {

    public static String transTerm(Node term, String resultVar, CodeGenContext ctx) {
        // TODO: dispatch on the shape of the TERM node:
        //   NAME            -> resultVar = <ctx.lookUp(name)>;
        //   NUM             -> resultVar = NUM;
        //   CALL            -> 3b, throw UnsupportedOperationException("3b")
        //   add/sub/mul/div/mod ( TERM TERM ) -> binary(...)
        //   neg ( TERM )    -> unary(...)
        throw new UnsupportedOperationException("TODO");
    }

    /** x = trans(left); y = trans(right); resultVar = x op y; */
    private static String binary(String op, Node left, Node right,
                                 String resultVar, CodeGenContext ctx) {
        // TODO: String x = ctx.newVar(); String y = ctx.newVar();
        //       code1 = transTerm(left, x, ctx); code2 = transTerm(right, y, ctx);
        //       return code1 + code2 + resultVar + " = " + x + " " + op + " " + y + ";\n";
        throw new UnsupportedOperationException("TODO");
    }

    /** x = trans(inner); resultVar = -x; */
    private static String unary(Node inner, String resultVar, CodeGenContext ctx) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /** add + , sub - , mul * , div / , mod % */
    static String opSymbol(String splOp) {
        switch (splOp) {
            case "add": return "+";
            case "sub": return "-";
            case "mul": return "*";
            case "div": return "/";
            case "mod": return "%";
            default: throw new IllegalArgumentException("Unknown op: " + splOp);
        }
    }
}