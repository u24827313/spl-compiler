package spl.semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final NameGenerator names;
    private final Deque<Scope> scopes = new ArrayDeque<>();
    private final Map<Integer, SymbolEntry> entriesByNodeId = new HashMap<>();

    SymbolTable(NameGenerator names) {
        this.names = names;
    }

    NameGenerator names() {
        return names;
    }

    void pushScope(int level) {
        Scope newScope = new Scope(level) ;
        scopes.push(newScope) ;
    }

    void popScope() {
        scopes.pop();
    }

    Scope currentScope() {
        return scopes.peek();
    }

    SymbolEntry lookupVar(String name) {
        for (Scope scope : scopes) {
            SymbolEntry entry = scope.getVar(name);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    SymbolEntry lookupFunc(String name) {
        Scope scope = currentScope();
        if (scope == null){
            return null ;
        }

        return scope.getFunc(name) ;
    }

    void record(int nodeId, SymbolEntry entry) {
        entriesByNodeId.put(nodeId, entry);
    }

    public SymbolEntry getEntry(int nodeId) {
        return entriesByNodeId.get(nodeId);
    }
}
