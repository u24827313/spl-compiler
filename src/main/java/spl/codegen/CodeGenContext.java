package spl.codegen;

import spl.semantic.SymbolEntry;
import spl.semantic.SymbolTable;
import spl.tree.Node;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared state for code generation (3a and 3b). One instance is created in
 * CodeGenMain and passed to every translator.
 *
 * Uses the real spl.tree.Node and spl.semantic.SymbolTable.
 */
public class CodeGenContext {

    private int tempCounter = 0;
    private int labelCounter = 0;
    private final List<String> tempVars = new ArrayList<>();

    /** 3b: user variables (main's, plus function parameters and locals), declared once. */
    private final Set<String> userVars = new LinkedHashSet<>();

    /** 3b: how deep we are inside nested inlining (guard against runaway recursion). */
    private int inlineDepth = 0;
    private static final int MAX_INLINE_DEPTH = 100;

    /** "int" or "float", as decided by the type analyser (2b). */
    private final String cType;
    private final SymbolTable symT;

    /** 3b: null when the context was built by the 2-arg (3a) constructor. */
    private final FunctionRegistry registry;

    /** 3a constructor, kept so existing code and tests keep compiling. */
    public CodeGenContext(SymbolTable symT, String cType) {
        this(symT, cType, null);
    }

    /** 3b constructor. */
    CodeGenContext(SymbolTable symT, String cType, FunctionRegistry registry) {
        this.symT = symT;
        this.cType = cType;
        this.registry = registry;
    }

    /** New temporary. Prefix "t" must not clash with 2a internal names. */
    public String newVar() {
        String name = "t" + (++tempCounter);
        tempVars.add(name);
        return name;
    }

    /**
     * New temporary whose name starts with prefix (3b: the return variable
     * r = f ++ n, e.g. newVar("f1") -> "f1t3"). Registered for hoisting like
     * newVar(), so it is declared exactly once.
     */
    public String newVar(String prefix) {
        String name = prefix + "t" + (++tempCounter);
        tempVars.add(name);
        return name;
    }

    /** New label. Never reused (keeps inlining safe in 3b). */
    public String newLabel() {
        return "L" + (++labelCounter);
    }

    /** NAME node -> internal (renamed) variable name from the symbol table. */
    public String lookUp(Node nameNode) {
        // Scopes are already popped after 2a, so lookupVar(name) cannot be used here.
        // Use the per-node record instead: getEntry(nodeId) is public.
        SymbolEntry e = symT.getEntry(nameNode.getId());
        if (e == null) {
            throw new IllegalStateException("No symbol entry for NAME node " + nameNode.getId()
                    + " (" + nameNode.getContents() + ")");
        }
        return e.getUniqueName();
    }

    /** 3b: register a user variable (internal name, e.g. "v3") for declaration. Duplicates are ignored. */
    public void declareUserVar(String uniqueName) {
        userVars.add(uniqueName);
    }

    /** 3b: the function registry; fails loudly if this context was built without one. */
    FunctionRegistry registry() {
        if (registry == null) {
            throw new IllegalStateException("This CodeGenContext has no FunctionRegistry (3b not wired in)");
        }
        return registry;
    }

    /** 3b: the symbol table (CallGen needs it for registry().forCall(call, symT)). */
    public SymbolTable symbolTable() {
        return symT;
    }

    /**
     * 3b: a fresh context (own temp and label counters, own declaration lists)
     * sharing symT, cType and registry. Used to generate the commented code-mine
     * without polluting the real context.
     */
    public CodeGenContext scratch() {
        return new CodeGenContext(symT, cType, registry);
    }

    /** 3b: call when entering an inlined call; throws if nesting gets absurdly deep. */
    public void beginInline() {
        if (++inlineDepth > MAX_INLINE_DEPTH) {
            throw new IllegalStateException("Inlining nested deeper than " + MAX_INLINE_DEPTH
                    + " levels (recursive call?)");
        }
    }

    /** 3b: call (in a finally block) when leaving an inlined call. */
    public void endInline() {
        inlineDepth--;
    }

    /**
     * All declarations: user variables first, then temps, e.g.
     * "int v1;\nint t1;\n". Call once, after the body has been generated.
     */
    public String declarations() {
        StringBuilder sb = new StringBuilder();
        for (String v : userVars) {
            sb.append(cType).append(' ').append(v).append(";\n");
        }
        for (String v : tempVars) {
            sb.append(cType).append(' ').append(v).append(";\n");
        }
        return sb.toString();
    }
}