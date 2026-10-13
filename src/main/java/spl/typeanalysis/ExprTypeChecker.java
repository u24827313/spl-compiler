package spl.typeanalysis;

import spl.tree.Node; 

/**
 * Owner: Person 2.  Rules: TERM, BOOL, CALL, INPUT.   (Reads name types, never writes them.)
 *
 *  TERM  -> NAME                         NAME numeric => TERM numeric
 *  TERM  -> NUM                          TERM numeric
 *  TERM  -> CALL                         CALL numeric => TERM numeric
 *  TERM  -> mod|div|add|sub|mul ( TERM TERM )   both numeric => numeric
 *  TERM  -> neg ( TERM )                 numeric => numeric
 *  BOOL  -> not ( BOOL )                 boolean => boolean
 *  BOOL  -> and|or ( BOOL BOOL )         both boolean => boolean
 *  BOOL  -> eq|larger|lesser ( TERM TERM )   both numeric => boolean
 *  CALL  -> NAME ( INPUT )               INPUT ok and type_of(NAME) != unknown
 *                                        => type_of(CALL) = type_of(NAME)
 *  INPUT -> eps                          ok
 *  INPUT -> TERM INPUT                   TERM numeric, INPUT ok => INPUT ok
 *
 * Child layout in the tree:
 *  TERM:  [NAME] | [NUM] | [CALL] | [op, (, TERM, TERM, )] | [neg, (, TERM, )]
 *  BOOL:  [not, (, BOOL, )] | [and|or, (, BOOL, BOOL, )] | [eq|larger|lesser, (, TERM, TERM, )]
 *  CALL:  [NAME, (, INPUT, )]       INPUT: [] | [TERM, INPUT]
 *  A one-child TERM is told apart by ctx.kind(child): "NAME", "NUM" or "CALL".
 *  A NAME leaf under TERM is a USE: ctx.check(leaf) returns its recorded type.
 *
 * Which variant of TERM/BOOL a node is: look at the operator child (ctx.isOperator).
 */
public class ExprTypeChecker {

    private final TypeContext ctx;

    public ExprTypeChecker(TypeContext ctx) {
        this.ctx = ctx;
    }

    public Type check(Node n) {
        switch (ctx.kind(n)) {
            case "TERM":  return checkTerm(n);
            case "BOOL":  return checkBool(n);
            case "CALL":  return checkCall(n);
            case "INPUT": return checkInput(n);
            default:
                throw ctx.error(n, "ExprTypeChecker cannot handle " + ctx.kind(n));
        }
    }

    private Type checkTerm(Node n)  { throw new UnsupportedOperationException("TODO"); }
    private Type checkBool(Node n)  { throw new UnsupportedOperationException("TODO"); }
    private Type checkCall(Node n)  { throw new UnsupportedOperationException("TODO"); }
    private Type checkInput(Node n) { throw new UnsupportedOperationException("TODO"); }
}