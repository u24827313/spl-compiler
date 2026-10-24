package spl.semantic;
import spl.tree.*;

public class FunctionChecker {
    void declareFunction(Node nameNode, SymbolTable t){
        String name = nameNode.getContents();
        Scope scope = t.currentScope();

        if(scope.getFunc(name) != null){
            throw new SemanticException(
                SemanticErrorType.DUPLICATE_FUNCTION,
                "The function '" + name + "' has already been declared in this scope.",
                nameNode.getId()
            );
        }

        if(scope.getVar(name) != null) {
            throw new SemanticException(
                SemanticErrorType.NAME_CONFLICT,
                "The name '" + name + "' is already used by a variable in this scope.",
                nameNode.getId()
            );
        }

        String uniqueName = t.names().next(SymbolEntry.Kind.FUNC);

        SymbolEntry entry = new SymbolEntry(
            name,
            SymbolEntry.Kind.FUNC,
            scope.level,
            nameNode.getId(),
            uniqueName
        );

        // Register function in the current scope
        scope.putFunc(name, entry);

        // Associate the declaration node with its symbol
        t.record(nameNode.getId(), entry);

    }

    void resolveFunctionCall(Node nameNode, SymbolTable t){
        String name = nameNode.getContents();
        
        // Function lookup must only search the current scope
        SymbolEntry entry = t.lookupFunc(name);

        if(entry == null) {
            throw new SemanticException(
                SemanticErrorType.UNDECLARED_FUNCTION,
                "The function '" + name + "' has not been declared in the current scope.",
                nameNode.getId()
            );
        }

        // Link this function call to its existing declaration
        t.record(nameNode.getId(), entry);
    }
}
