package spl.semantic;

import spl.tree.SyntaxTree;

/** Contract-facing entry point; the existing SemanticAnalyzer remains available to current callers. */
public class SemanticAnalyser {
    public SymbolTable analyse(SyntaxTree tree) {
        return new SemanticAnalyzer().analyse(tree.getRoot());
    }
}
