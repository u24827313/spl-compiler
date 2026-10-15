package spl.semantic;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    final int level;

    private final Map<String, SymbolEntry> variables = new HashMap<>();
    private final Map<String, SymbolEntry> functions = new HashMap<>();

    Scope(int level) {
        this.level = level;
    }

    void putVar(String name, SymbolEntry entry) {
        variables.put(name, entry);
    }

    SymbolEntry getVar(String name) {
        SymbolEntry var = variables.get(name) ;
        return var ;
    }

    void putFunc(String name, SymbolEntry entry) {
        functions.put(name, entry);
    }

    SymbolEntry getFunc(String name) {
        SymbolEntry func = functions.get(name) ;
        return func;
    }

    boolean hasName(String name) {
        boolean containsVar = variables.containsKey(name) ;
        boolean containsFunc = functions.containsKey(name) ;
        return containsVar || containsFunc ;
    }
}
