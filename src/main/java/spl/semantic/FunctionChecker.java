package spl.semantic;
import spl.tree.*;

public class FunctionChecker {
    void declareFunction(Node nameNode, SymbolTable t){

    }    // duplicate in this scope -> SemanticException
    void resolveFunctionCall(Node nameNode, SymbolTable t){

    } // not in top scope -> SemanticException
}
