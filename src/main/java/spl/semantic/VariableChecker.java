package spl.semantic;
import spl.tree.*;

public class VariableChecker {
    void declareParameter(Node nameNode, SymbolTable t){

    }
    void declareVariable(Node nameNode, SymbolTable t){

    }   // duplicate / masks parameter -> SemanticException

    void resolveVariableUse(Node nameNode, SymbolTable t){

    } // no declaration -> SemanticException
}
