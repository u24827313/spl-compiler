package spl.typeanalysis;

import spl.tree.Node; // TODO: adjust to the real package of Node

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared state and dispatcher for the type analyser (same idea as ParserContext).
 *
 *  - holds the type attribute of every node (kept here, so Node.java is NOT modified)
 *  - holds the type of every system-generated NAME (the 2b "symbol table" information)
 *  - routes check(node) to the checker that owns that node kind
 *  - is the ONLY place that touches the Node API (see "tree access" below)
 */
public class TypeContext {

    private final Map<Node, Type> nodeTypes = new IdentityHashMap<>();
    private final Map<String, Type> nameTypes = new HashMap<>();

    private final ProgramTypeChecker program;
    private final ExprTypeChecker expr;
    private final InstrTypeChecker instr;

    public TypeContext() {
        this.program = new ProgramTypeChecker(this);
        this.expr = new ExprTypeChecker(this);
        this.instr = new InstrTypeChecker(this);
    }

    // ------------------------------------------------------------------
    // Tree access: the single adaptation point to Node.java.
    // Node only has one string, getContents(), so:
    //   - internal node -> contents is the grammar symbol ("TERM", "P", ...)
    //   - leaf          -> contents is the token text; we classify it ourselves
    // ------------------------------------------------------------------

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "mod", "div", "add", "sub", "mul", "neg", "not", "and", "or",
            "eq", "larger", "lesser", "print", "nop", "comment", "if", "then",
            "else", "while", "until", "do", "void", "num", "return"));

    /** Grammar symbol of the node: "TERM", "NAME", "NUM", "STRING", a keyword, or punctuation. */
    public String kind(Node n) {
        String c = n.getContents();
        if (!n.isLeaf()) {
            return c;                                   // internal node: grammar symbol
        }
        if (KEYWORDS.contains(c)) {
            return c;                                   // keyword leaf: kind is the keyword
        }
        if (c.matches("-?\\d+(\\.\\d+)?")) {
            return "NUM";                               // VERIFY against your lexer's NUM format
        }
        if (c.length() >= 2 && c.startsWith("\"") && c.endsWith("\"")) {
            return "STRING";                            // VERIFY: are quotes kept in contents?
        }
        if (c.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return "NAME";                              // VERIFY against the 2a new-name format
        }
        return c;                                       // punctuation: ( ) { } ; : =
    }

    /** Children in grammar order. */
    public List<Node> children(Node n) {
        return n.getChildren();
    }

    /** Token text (new name of a NAME, text of a NUM, ...). */
    public String text(Node n) {
        return n.getContents();
    }

    /** True if the node is the given keyword/operator, e.g. isOperator(n, "mod"). */
    public boolean isOperator(Node n, String op) {
        return op.equals(n.getContents());
    }

    // ------------------------------------------------------------------
    // Node types
    // ------------------------------------------------------------------

    public Type typeOf(Node n) {
        return nodeTypes.getOrDefault(n, Type.UNKNOWN);
    }

    public void setType(Node n, Type t) {
        nodeTypes.put(n, t);
    }

    // ------------------------------------------------------------------
    // NAME types (keyed by the system-generated new name from phase 2a)
    // TODO: once the 2a SymbolTable API is confirmed, forward these two
    //       methods to it so the symbol table itself carries the types.
    // ------------------------------------------------------------------

    public Type nameType(String newName) {
        return nameTypes.getOrDefault(newName, Type.UNKNOWN);
    }

    public void setNameType(String newName, Type t) {
        nameTypes.put(newName, t);
    }

    // ------------------------------------------------------------------
    // Dispatch
    // ------------------------------------------------------------------

    /** Type-checks a node (and its subtree), records and returns its type. */
    public Type check(Node n) {
        Type t;
        switch (kind(n)) {
            case "SPL_PROG": case "P": case "V_DECL": case "F_DECL":
            case "F_TYPE": case "ALGO":
                t = program.check(n);
                break;
            case "TERM": case "BOOL": case "CALL": case "INPUT":
                t = expr.check(n);
                break;
            case "INSTR": case "OUTP": case "ASSIGN": case "BRANCH":
            case "LOOP": case "COND":
                t = instr.check(n);
                break;
            case "NAME":
                t = nameType(text(n));   // leaf: whatever the declaration gave it
                break;
            case "NUM":
                t = Type.NUMERIC;        // leaf
                break;
            case "STRING":
                t = Type.OK;             // leaf
                break;
            default:
                throw error(n, "Type analyser does not know node kind '" + kind(n) + "'");
        }
        setType(n, t);
        return t;
    }

    // ------------------------------------------------------------------
    // Error helpers
    // ------------------------------------------------------------------

    public TypeException error(Node n, String message) {
        return new TypeException(message, n);
    }

    /** Throws unless actual == expected. "rule" names the grammar rule for the message. */
    public void require(Node n, Type actual, Type expected, String rule) {
        if (actual != expected) {
            throw error(n, "Type error in rule " + rule + ": expected "
                    + expected.label() + " but found " + actual.label());
        }
    }
}