package spl.semantic;

import spl.tree.Node;

public class VariableChecker {

    private void checkParamConflict(Node nameNode , Scope scope){
        String name = nameNode.getContents();
        SymbolEntry exisiting = scope.getVar(name) ; 

        if (exisiting == null) {
            if (!scope.hasName(name)){
                return ;
            }
            throw new SemanticException("Paramter name conflicts with function: '" + name + "'" , nameNode.getId());
        }

        String message = "" ;
        if (exisiting.kind == SymbolEntry.Kind.PARAM){
            message = "Duplicate paramater declaration: '" + name + "'" ;
        }
        else {
            message = "Parameter name clashes with variable: '" + name + "'" ;
        }

        throw new SemanticException(message,nameNode.getId()) ; 
    }

    void declareParameter(Node nameNode, SymbolTable table) {
        Scope scope = table.currentScope();
        checkParamConflict(nameNode, scope);
        declare(nameNode, table, SymbolEntry.Kind.PARAM);
    }

    private void checkVarConflict(Node nameNode , Scope scope) { 
        String name = nameNode.getContents();

        if (!scope.hasName(name)){
            return ;
        }

        SymbolEntry existing = scope.getVar(name);
        String message = "" ;

        if (existing == null){
            message = "Variable name clashes with function: '" + name + "'";
        }
        else if (existing.kind == SymbolEntry.Kind.PARAM) {
            message = "Local variable name masks parameter: '" + name + "'" ;
        }
        else {
            message = "Duplicate variable declaration: '" + name + "'" ;
        }

        int nodeId = nameNode.getId();
        throw new SemanticException(message, nodeId);
        
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
            throw new SemanticException("Undeclared variable: '" + nameNode.getContents() + "'", nameNode.getId());
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
