package spl.semantic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NameGeneratorTest {

    @Test
    void generatesUniqueVariableNames() {
        NameGenerator generator = new NameGenerator();

        assertEquals("v1", generator.next(SymbolEntry.Kind.VAR));
        assertEquals("v2", generator.next(SymbolEntry.Kind.VAR));
        assertEquals("v3", generator.next(SymbolEntry.Kind.VAR));
    }

    @Test
    void parametersShareVariableCounter() {
        NameGenerator generator = new NameGenerator();

        assertEquals("v1", generator.next(SymbolEntry.Kind.VAR));
        assertEquals("v2", generator.next(SymbolEntry.Kind.PARAM));
        assertEquals("v3", generator.next(SymbolEntry.Kind.VAR));
    }

    @Test
    void functionsHaveIndependentCounter() {
        NameGenerator generator = new NameGenerator();

        assertEquals("v1", generator.next(SymbolEntry.Kind.VAR));
        assertEquals("f1", generator.next(SymbolEntry.Kind.FUNC));
        assertEquals("v2", generator.next(SymbolEntry.Kind.PARAM));
        assertEquals("f2", generator.next(SymbolEntry.Kind.FUNC));
    }
}