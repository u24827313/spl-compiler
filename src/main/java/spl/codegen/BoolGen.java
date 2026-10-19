package spl.codegen;

import static spl.codegen.GenKinds.*;

import java.util.List;
import spl.tree.Node;

/**
 * Person B: translation of BOOL into gotos, without else.
 *
 * CONVENTION (the one the whole generator relies on):
 *   transBool(b, target, dummy) returns code that jumps to `target` when b is
 *   TRUE, and FALLS THROUGH to the next line of code when b is FALSE.
 *   The caller places the false-continuation directly after this code.
 *
 * NOTE ON not / and: the sheet's literal rules (swap the labels for not; two
 * 'not' translations in a row for and) do not work with this convention: for
 * not(eq(x y)) the swapped code is just "if (x == y) goto <false>;", which
 * falls through into the false branch as well, so the true case is lost.
 * not and and therefore use one fresh label each, with an explicit goto.
 * Simple conditions (eq, larger, lesser, or) come out exactly as on the sheet.
 * The dummy parameter is kept for signature compatibility and is not needed.
 *
 * Depends on TermGen.transTerm.
 */
public class BoolGen {

    public static String transBool(Node bool, String targetLabel,
                                   String dummyLabel, CodeGenContext ctx) {
        if (bool.getChildren().isEmpty()) {
            throw new IllegalStateException("BOOL node " + bool.getId() + " has no children");
        }
        String op = leafText(bool.getChildren().get(0));
        List<Node> terms = childrenNamed(bool, TERM);
        List<Node> bools = childrenNamed(bool, BOOL);

        switch (op) {
            case "eq": case "larger": case "lesser":
                return comparison(op, terms.get(0), terms.get(1), targetLabel, ctx);

            case "not":
                return transNot(bools.get(0), targetLabel, dummyLabel, ctx);

            case "or":
                // B1 true -> target; else fall into B2; B2 true -> target; else fall out.
                return transBool(bools.get(0), targetLabel, dummyLabel, ctx)
                     + transBool(bools.get(1), targetLabel, dummyLabel, ctx);

            case "and": {
                // B1 true -> go on to B2; B1 false -> leave (false).
                // B2 true -> target; B2 false -> fall out (false).
                String second = ctx.newLabel();
                String leave = ctx.newLabel();
                return transBool(bools.get(0), second, dummyLabel, ctx)
                     + "goto " + leave + ";\n"
                     + label(second, "")
                     + transBool(bools.get(1), targetLabel, dummyLabel, ctx)
                     + label(leave, "");
            }
            default:
                throw new IllegalArgumentException("Unknown BOOL operator: " + op);
        }
    }

    /**
     * Code for not(inner): jump to target when inner is FALSE, fall through
     * when inner is TRUE. Also used directly by while and do-until.
     */
    static String transNot(Node inner, String targetLabel, String dummyLabel, CodeGenContext ctx) {
        String skip = ctx.newLabel();
        return transBool(inner, skip, dummyLabel, ctx)   // inner true -> skip (= false, fall out)
             + "goto " + targetLabel + ";\n"             // inner false -> true case
             + label(skip, "");
    }

    /** x = trans(t1); y = trans(t2); if ( x op y ) goto label; */
    private static String comparison(String splOp, Node t1, Node t2,
                                     String label, CodeGenContext ctx) {
        String x = ctx.newVar();
        String y = ctx.newVar();
        String code1 = TermGen.transTerm(t1, x, ctx);
        String code2 = TermGen.transTerm(t2, y, ctx);
        return code1 + code2 + "if ( " + x + " " + opSymbol(splOp) + " " + y + " ) goto " + label + ";\n";
    }

    /** eq == , larger > , lesser < */
    static String opSymbol(String splOp) {
        switch (splOp) {
            case "eq":     return "==";
            case "larger": return ">";
            case "lesser": return "<";
            default: throw new IllegalArgumentException("Unknown op: " + splOp);
        }
    }
}