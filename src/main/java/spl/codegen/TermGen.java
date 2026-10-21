package spl.codegen;

import static spl.codegen.GenKinds.*;

import java.util.List;
import spl.tree.Node;


/**
 * Person A: translation of TERM (Textbook Fig. 6.3 style).
 *
 * Contract: transTerm writes its result into resultVar and returns the C code
 * as a String. It never returns the value itself.
 */
public class TermGen {

    public static String transTerm(Node term, String resultVar, CodeGenContext ctx) {
        if (term.getChildren().isEmpty()) {
            throw new IllegalStateException("TERM node " + term.getId() + " has no children");
        }

        Node first = term.getChildren().get(0);
        if(is(first, CALL)){
            return CallGen.transCallTerm(first, resultVar, ctx);
        }
        if(isUserDefinedName(first)){
            return resultVar + " = " + ctx.lookUp(first) + ";\n";
        }

        String tok = leafText(first);
        List<Node> operands = childrenNamed(term, TERM);
        switch(tok){
            case "add": case "sub": case "mul": case "div": case "mod":
                requireOperandCount(term, operands, 2);
                return binary(opSymbol(tok), operands.get(0), operands.get(1), resultVar, ctx);
            case "neg":
                requireOperandCount(term, operands, 1);
                return unary(operands.get(0), resultVar, ctx);
            default:
                if (tok.matches("-?\\d+(\\.\\d+)?")) {
                    return resultVar + " = " + tok + ";\n";
                }
                throw new IllegalArgumentException("Unknown TERM alternative: " + tok);
        }

    }

    private static void requireOperandCount(Node term, List<Node> operands, int expected) {
        if (operands.size() != expected) {
            throw new IllegalStateException("Malformed TERM node " + term.getId()
                    + ": expected " + expected + " operands but found " + operands.size());
        }
    }

    /** x = trans(left); y = trans(right); resultVar = x op y; */
    private static String binary(String op, Node left, Node right, String resultVar, CodeGenContext ctx) {
        String x = ctx.newVar();
        String y = ctx.newVar();
        String code1 = transTerm(left, x, ctx);
        String code2 = transTerm(right, y, ctx);
        return code1 + code2 + resultVar + " = " + x + " " + op + " " + y + ";\n";
    }
        

    /** x = trans(inner); resultVar = -x; */
    private static String unary(Node inner, String resultVar, CodeGenContext ctx) {
        String x = ctx.newVar();
        String code = transTerm(inner, x, ctx);
        return code + resultVar + " = -" + x + ";\n";
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