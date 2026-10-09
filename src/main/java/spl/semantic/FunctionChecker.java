package spl.semantic;
import spl.tree.*;

public class FunctionChecker {
    void declareFunction(Node nameNode, SymbolTable t){
        String name = nameNode.getContents();
        Scope scope = t.currentScope();

        if(scope.getFunc(name) != null){
            throw new SemanticException(
                "Duplicate function declaration: '" + name + "'",
                nameNode.getId()       
            );
        }

        if(scope.getVar(name) != null) {
            throw new SemanticException(
                "Function name clashes with variable: '" + name + "'",
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
                "Function '" + name + "' is not declared in the current scope",
                nameNode.getId()
            );
        }

        // Link this function call to its existing declaration
        t.record(nameNode.getId(), entry);
    }
}
