package spl.codegen;

import spl.semantic.SymbolTable;
import spl.tree.Node;

import java.util.HashMap;
import java.util.Map;


final class FunctionRegistry {

    /** key = id of the #name leaf in the F_TYPE (= SymbolEntry.getDeclNodeId()). */
    private final Map<Integer, FunctionInfo> byDeclNodeId = new HashMap<>();

    FunctionRegistry() {}

    /** Crawl the whole SPL_PROG tree and register every F_TYPE. */
    static FunctionRegistry build(Node splProgRoot, SymbolTable symT) {
        // TODO (Person 2):
        //   for each F_TYPE node f anywhere in the tree:
        //     nameLeaf   = GenKinds.firstNameChild(f)
        //     entry      = symT.getEntry(nameLeaf.getId())        -> uniqueName
        //     isVoid     = first child of f is the leaf "void"
        //     params     = GenKinds.namesInVDecl(childNamed(f, V_DECL))
        //     bodyP      = childNamed(f, P)
        //     returnTerm = childNamed(f, TERM)   (null for void)
        //     byDeclNodeId.put(nameLeaf.getId(), new FunctionInfo(...))
        throw new UnsupportedOperationException("TODO (3b, Person 2): FunctionRegistry.build");
    }

    /** The FunctionInfo of the function that this CALL node invokes. */
    FunctionInfo forCall(Node callNode, SymbolTable symT) {
        // TODO (Person 2):
        //   callName = GenKinds.firstNameChild(callNode)
        //   entry    = symT.getEntry(callName.getId())      (null -> IllegalStateException)
        //   return byDeclNodeId.get(entry.getDeclNodeId())  (null -> IllegalStateException)
        throw new UnsupportedOperationException("TODO (3b, Person 2): FunctionRegistry.forCall");
    }
}