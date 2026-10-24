package spl.semantic;

import spl.tree.Node;

public class VariableChecker {

    private void checkParamConflict(Node nameNode, Scope scope) {
        String name = nameNode.getContents();
        SymbolEntry existingVariable = scope.getVar(name);

        if (existingVariable == null && !scope.hasName(name)) {
            return;
        }

        if (existingVariable == null) {
            String message = "The name '" + name + "' is already used by a function in this scope.";
            throw new SemanticException(SemanticErrorType.NAME_CONFLICT, message, nameNode.getId());
        }

        if (existingVariable.kind == SymbolEntry.Kind.PARAM) {
            String message = "The parameter '" + name + "' has already been declared in this function.";
            throw new SemanticException(SemanticErrorType.DUPLICATE_PARAMETER, message, nameNode.getId());
        }

        String message = "The name '" + name + "' is already used by a variable in this scope.";
        throw new SemanticException(SemanticErrorType.NAME_CONFLICT, message, nameNode.getId());
    }

    void declareParameter(Node nameNode, SymbolTable table) {
        Scope scope = table.currentScope();
        checkParamConflict(nameNode, scope);
        declare(nameNode, table, SymbolEntry.Kind.PARAM);
    }

    private void checkVarConflict(Node nameNode, Scope scope) {
        String name = nameNode.getContents();
        if (!scope.hasName(name)) {
            return;
        }

        SymbolEntry existingVariable = scope.getVar(name);
        if (existingVariable == null) {
            String message = "The name '" + name + "' is already used by a function in this scope.";
            throw new SemanticException(SemanticErrorType.NAME_CONFLICT, message, nameNode.getId());
        }

        if (existingVariable.kind == SymbolEntry.Kind.PARAM) {
            String message = "The local variable '" + name + "' cannot use the same name as a parameter.";
            throw new SemanticException(
                    SemanticErrorType.INVALID_LOCAL_VARIABLE_NAME,
                    message,
                    nameNode.getId()
            );
        }

        String message = "The variable '" + name + "' has already been declared in this scope.";
        throw new SemanticException(SemanticErrorType.DUPLICATE_VARIABLE, message, nameNode.getId());
    }

    void declareVariable(Node nameNode, SymbolTable table) {
        Scope scope = table.currentScope();
        checkVarConflict(nameNode, scope);
        declare(nameNode, table, SymbolEntry.Kind.VAR);
    }

    void resolveVariableUse(Node nameNode, SymbolTable table) {
        String name = nameNode.getContents() ;
        SymbolEntry entry = table.lookupVar(name);
        if (entry == null) {
            String message = "The variable '" + name + "' has not been declared in an accessible scope.";
            throw new SemanticException(
                    SemanticErrorType.UNDECLARED_VARIABLE,
                    message,
                    nameNode.getId()
            );
        }
        table.record(nameNode.getId(), entry);
    }

    private void declare(Node nameNode, SymbolTable table, SymbolEntry.Kind kind) {
        Scope scope = table.currentScope();

        String originalName = nameNode.getContents() ;

        NameGenerator nameGenerator = table.names() ;
        String newName = nameGenerator.next(kind) ;
        int nodeId = nameNode.getId() ;

        SymbolEntry entry = new SymbolEntry(originalName, kind, scope.level, nodeId, newName) ;

        scope.putVar(originalName, entry);
        scope.putVar(originalName, entry);
        table.record(nodeId, entry);
    }
}
