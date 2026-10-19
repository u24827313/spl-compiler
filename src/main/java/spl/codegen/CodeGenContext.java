package spl.codegen;

import spl.semantic.SymbolEntry;
import spl.semantic.SymbolTable;
import spl.tree.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared state for Phase 3a code generation. One instance is created in
 * CodeGenMain and passed to every translator.
 *
 * Uses the real spl.tree.Node and spl.semantic.SymbolTable.
 */
public class CodeGenContext {

    private int tempCounter = 0;
    private int labelCounter = 0;
    private final List<String> tempVars = new ArrayList<>();

    /** "int" or "float", as decided by the type analyser (2b). */
    private final String cType;
    private final SymbolTable symT;

    public CodeGenContext(SymbolTable symT, String cType) {
        this.symT = symT;
        this.cType = cType;
    }

    /** New temporary. Prefix "t" must not clash with 2a internal names. */
    public String newVar() {
        String name = "t" + (++tempCounter);
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

    /** All temp declarations, e.g. "int t1;\nint t2;\n". Call once, at the end. */
    public String declarations() {
        StringBuilder sb = new StringBuilder();
        for (String v : tempVars) {
            sb.append(cType).append(' ').append(v).append(";\n");
        }
        return sb.toString();
    }
}