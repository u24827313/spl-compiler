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

    private Type checkTerm(Node n) {
        var children = ctx.children(n);

        if (children.size() == 1) {
            Node child = children.get(0);
            String kind = ctx.kind(child);

            if (!kind.equals("NAME")
                    && !kind.equals("NUM")
                    && !kind.equals("CALL")) {
                throw ctx.error(n, "Unexpected TERM child: " + kind);
            }

            ctx.require(child, ctx.check(child), Type.NUMERIC, "TERM -> " + kind);
            return Type.NUMERIC;
        }

        if (children.isEmpty()) {
            throw ctx.error(n, "TERM has no children");
        }

        String operator = ctx.text(children.get(0));

        switch (operator) {
            case "neg":
                requireChildCount(n, 4);
                requireChildType(n, 2, Type.NUMERIC, "TERM -> neg ( TERM )");
                break;

            case "mod":
            case "div":
            case "add":
            case "sub":
            case "mul":
                requireChildCount(n, 5);
                requireChildType(n, 2, Type.NUMERIC, "TERM -> " + operator + " ( TERM TERM )");
                requireChildType(n, 3, Type.NUMERIC, "TERM -> " + operator + " ( TERM TERM )");
                break;

            default:
                throw ctx.error(n, "Unknown TERM operator: " + operator);
        }

        return Type.NUMERIC;
    }

    private Type checkBool(Node n) {
        var children = ctx.children(n);

        if (children.isEmpty()) {
            throw ctx.error(n, "BOOL has no children");
        }

        String operator = ctx.text(children.get(0));
        String rule = "BOOL -> " + operator;

        switch (operator) {
            case "not":
                requireChildCount(n, 4);
                requireChildType(n, 2, Type.BOOLEAN, rule);
                break;

            case "and":
            case "or":
                requireChildCount(n, 5);
                requireChildType(n, 2, Type.BOOLEAN, rule);
                requireChildType(n, 3, Type.BOOLEAN, rule);
                break;

            case "eq":
            case "larger":
            case "lesser":
                requireChildCount(n, 5);
                requireChildType(n, 2, Type.NUMERIC, rule);
                requireChildType(n, 3, Type.NUMERIC, rule);
                break;

            default:
                throw ctx.error(n, "Unknown BOOL operator: " + operator);
        }

        return Type.BOOLEAN;
    }

    private Type checkCall(Node n) {
        requireChildCount(n, 4);

        Node name = ctx.children(n).get(0);
        Node input = ctx.children(n).get(2);

        ctx.require(input, ctx.check(input), Type.OK, "CALL -> NAME ( INPUT )");

        Type functionType = ctx.check(name);

        if (functionType != Type.NUMERIC && functionType != Type.PROCEDURE) {
            String message = "The name '" + ctx.text(name) + "' cannot be called because it has type '"
                    + functionType.label() + "'. Only numeric functions and procedures can be called.";
            throw ctx.error(name, TypeErrorType.INVALID_FUNCTION_USE, message);
        }

        return functionType;
    }

    private Type checkInput(Node n) {
        if (ctx.children(n).isEmpty()) {
            return Type.OK;
        }

        requireChildCount(n, 2);
        requireChildType(n, 0, Type.NUMERIC, "INPUT -> TERM INPUT");
        requireChildType(n, 1, Type.OK, "INPUT -> TERM INPUT");

        return Type.OK;
    }

    private void requireChildType(
        Node parent, int index, Type expected, String rule) {
        Node child = ctx.children(parent).get(index);
        ctx.require(child, ctx.check(child), expected, rule);
    }

    private void requireChildCount(Node n, int expected) {
        int actual = ctx.children(n).size();

        if (actual != expected) {
            throw ctx.error(n, "Malformed " + ctx.kind(n) + ": expected "
                + expected + " children but found " + actual);
        }
    }
}