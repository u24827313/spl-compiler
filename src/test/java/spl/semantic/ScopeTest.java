package spl.semantic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopeTest {

    private SymbolTable table;
    private Scope scope;

    @BeforeEach
    void setUp() {
        table = new SymbolTable(new NameGenerator());
        table.pushScope(0);
        scope = table.currentScope();
    }

    @Test
    void storesTheLevelSuppliedWhenTheScopeIsPushed() {
        assertEquals(0, scope.level);

        table.pushScope(1);
        assertEquals(1, table.currentScope().level);
    }

    @Test
    void storesVariablesAndParametersByTheirOriginalNames() {
        SymbolEntry variable = entry("#x", SymbolEntry.Kind.VAR, 1);
        SymbolEntry parameter = entry("#arg", SymbolEntry.Kind.PARAM, 2);

        scope.putVar("#x", variable);
        scope.putVar("#arg", parameter);

        assertSame(variable, scope.getVar("#x"));
        assertSame(parameter, scope.getVar("#arg"));
        assertNull(scope.getVar("#missing"));
    }

    @Test
    void storesFunctionsSeparatelyFromVariables() {
        SymbolEntry function = entry("#helper", SymbolEntry.Kind.FUNC, 3);

        scope.putFunc("#helper", function);

        assertSame(function, scope.getFunc("#helper"));
        assertNull(scope.getVar("#helper"));
        assertNull(scope.getFunc("#missing"));
    }

    @Test
    void hasNameChecksBothKindsButOnlyThisScope() {
        scope.putVar("#x", entry("#x", SymbolEntry.Kind.VAR, 1));
        scope.putFunc("#helper", entry("#helper", SymbolEntry.Kind.FUNC, 2));

        assertTrue(scope.hasName("#x"));
        assertTrue(scope.hasName("#helper"));
        assertFalse(scope.hasName("#missing"));

        table.pushScope(1);
        assertFalse(table.currentScope().hasName("#x"));
        assertFalse(table.currentScope().hasName("#helper"));
    }

    private static SymbolEntry entry(String name, SymbolEntry.Kind kind, int id) {
        return new SymbolEntry(name, kind, 0, id, "system" + id);
    }
}
