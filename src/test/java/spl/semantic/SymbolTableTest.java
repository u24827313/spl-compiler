package spl.semantic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SymbolTableTest {

    private NameGenerator generator;
    private SymbolTable table;

    @BeforeEach
    void setUp() {
        generator = new NameGenerator();
        table = new SymbolTable(generator);
    }

    @Test
    void exposesTheProvidedNameGenerator() {
        assertSame(generator, table.names());
    }

    @Test
    void pushAndPopRestoreTheEnclosingScope() {
        table.pushScope(0);
        Scope outer = table.currentScope();
        assertEquals(0, outer.level);

        table.pushScope(1);
        assertEquals(1, table.currentScope().level);
        assertNotSame(outer, table.currentScope());

        table.popScope();
        assertSame(outer, table.currentScope());
    }

    @Test
    void variableLookupFindsAncestorsAndPrefersTheNearestDeclaration() {
        table.pushScope(0);
        SymbolEntry outer = entry("#x", SymbolEntry.Kind.VAR, 0, 1, "v1");
        table.currentScope().putVar("#x", outer);
        assertSame(outer, table.lookupVar("#x"));

        table.pushScope(1);
        assertSame(outer, table.lookupVar("#x"));
        SymbolEntry inner = entry("#x", SymbolEntry.Kind.PARAM, 1, 2, "v2");
        table.currentScope().putVar("#x", inner);
        assertSame(inner, table.lookupVar("#x"));

        table.popScope();
        assertSame(outer, table.lookupVar("#x"));
        assertNull(table.lookupVar("#missing"));
    }

    @Test
    void siblingScopesCannotSeeEachOthersVariables() {
        table.pushScope(0);
        table.pushScope(1);
        SymbolEntry sibling = entry("#local", SymbolEntry.Kind.VAR, 1, 3, "v1");
        table.currentScope().putVar("#local", sibling);
        table.record(3, sibling);
        table.popScope();

        table.pushScope(1);
        assertNull(table.lookupVar("#local"));
        assertSame(sibling, table.getEntry(3));
    }

    @Test
    void functionLookupOnlyChecksTheCurrentScope() {
        table.pushScope(0);
        SymbolEntry outer = entry("#helper", SymbolEntry.Kind.FUNC, 0, 4, "f1");
        table.currentScope().putFunc("#helper", outer);
        assertSame(outer, table.lookupFunc("#helper"));

        table.pushScope(1);
        assertNull(table.lookupFunc("#helper"));
        SymbolEntry inner = entry("#helper", SymbolEntry.Kind.FUNC, 1, 5, "f2");
        table.currentScope().putFunc("#helper", inner);
        assertSame(inner, table.lookupFunc("#helper"));

        table.popScope();
        assertSame(outer, table.lookupFunc("#helper"));
    }

    @Test
    void recordedDeclarationAndUseRemainAccessibleAfterScopeIsPopped() {
        table.pushScope(0);
        table.pushScope(1);
        SymbolEntry declaration = entry("#x", SymbolEntry.Kind.VAR, 1, 6, "v1");
        table.currentScope().putVar("#x", declaration);
        table.record(6, declaration);
        table.record(7, declaration);
        table.popScope();

        assertSame(declaration, table.getEntry(6));
        assertSame(declaration, table.getEntry(7));
        assertNull(table.lookupVar("#x"));
        assertNull(table.getEntry(99));
    }

    private static SymbolEntry entry(String name, SymbolEntry.Kind kind, int level, int id, String uniqueName) {
        return new SymbolEntry(name, kind, level, id, uniqueName);
    }
}
