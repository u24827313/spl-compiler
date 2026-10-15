package spl.semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolEntryTest {

    @Test
    void retainsDeclarationDetails() {
        SymbolEntry entry = new SymbolEntry("#count", SymbolEntry.Kind.VAR, 2, 17, "v3");

        assertEquals("#count", entry.originalName);
        assertEquals(SymbolEntry.Kind.VAR, entry.kind);
        assertEquals(2, entry.level);
        assertEquals(17, entry.declNodeId);
        assertEquals("v3", entry.uniqueName);
    }

    @Test
    void distinguishesParameterAndFunctionEntries() {
        SymbolEntry parameter = new SymbolEntry("#input", SymbolEntry.Kind.PARAM, 1, 4, "v1");
        SymbolEntry function = new SymbolEntry("#helper", SymbolEntry.Kind.FUNC, 0, 8, "f1");

        assertEquals(SymbolEntry.Kind.PARAM, parameter.kind);
        assertEquals("v1", parameter.uniqueName);
        assertEquals(SymbolEntry.Kind.FUNC, function.kind);
        assertEquals("f1", function.uniqueName);
    }

    @Test
    void descriptionIdentifiesTheDeclaration() {
        SymbolEntry entry = new SymbolEntry("#count", SymbolEntry.Kind.VAR, 2, 17, "v3");

        assertTrue(entry.toString().contains("#count"));
        assertTrue(entry.toString().contains("v3"));
        assertTrue(entry.toString().contains("VAR"));
        assertTrue(entry.toString().contains("2"));
        assertTrue(entry.toString().contains("17"));
    }
}
