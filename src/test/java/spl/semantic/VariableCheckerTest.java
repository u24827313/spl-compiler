package spl.semantic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import spl.tree.Node;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableCheckerTest {

    private VariableChecker checker;
    private SymbolTable table;

    @BeforeEach
    void setUp() {
        checker = new VariableChecker();
        table = new SymbolTable(new NameGenerator());
        table.pushScope(0);
    }

    @Test
    void declaresAndRecordsAVariableAtTheCurrentLevel() {
        Node declaration = new Node(1, "#count");

        checker.declareVariable(declaration, table);

        SymbolEntry entry = table.lookupVar("#count");
        assertEquals("#count", entry.originalName);
        assertEquals(SymbolEntry.Kind.VAR, entry.kind);
        assertEquals(0, entry.level);
        assertEquals(1, entry.declNodeId);
        assertEquals("v1", entry.uniqueName);
        assertSame(entry, table.getEntry(1));
    }

    @Test
    void declaresAndRecordsAParameterAtTheCurrentLevel() {
        table.pushScope(1);
        Node declaration = new Node(2, "#arg");

        checker.declareParameter(declaration, table);

        SymbolEntry entry = table.lookupVar("#arg");
        assertEquals("#arg", entry.originalName);
        assertEquals(SymbolEntry.Kind.PARAM, entry.kind);
        assertEquals(1, entry.level);
        assertEquals(2, entry.declNodeId);
        assertEquals("v1", entry.uniqueName);
        assertSame(entry, table.getEntry(2));
    }

    @Test
    void parametersAndVariablesShareOneNameSequence() {
        checker.declareVariable(new Node(1, "#outer"), table);
        table.pushScope(1);
        checker.declareParameter(new Node(2, "#arg"), table);
        checker.declareVariable(new Node(3, "#local"), table);

        assertEquals("v1", table.getEntry(1).uniqueName);
        assertEquals("v2", table.getEntry(2).uniqueName);
        assertEquals("v3", table.getEntry(3).uniqueName);
    }

    @Test
    void usesRecordTheExactDeclarationEntryNotANewOne() {
        checker.declareVariable(new Node(1, "#count"), table);

        checker.resolveVariableUse(new Node(2, "#count"), table);
        checker.resolveVariableUse(new Node(3, "#count"), table);

        assertSame(table.getEntry(1), table.getEntry(2));
        assertSame(table.getEntry(1), table.getEntry(3));
        assertEquals("v1", table.getEntry(3).uniqueName);
    }

    @Test
    void findsAnOuterVariableFromInsideAFunctionBody() {
        checker.declareVariable(new Node(1, "#outer"), table);
        table.pushScope(1);

        checker.resolveVariableUse(new Node(2, "#outer"), table);

        assertSame(table.getEntry(1), table.getEntry(2));
    }

    @Test
    void shadowingChoosesTheNearestVariableThenRestoresTheOuterOne() {
        checker.declareVariable(new Node(1, "#x"), table);
        table.pushScope(1);
        checker.declareVariable(new Node(2, "#x"), table);
        checker.resolveVariableUse(new Node(3, "#x"), table);

        assertNotSame(table.getEntry(1), table.getEntry(2));
        assertEquals("v1", table.getEntry(1).uniqueName);
        assertEquals("v2", table.getEntry(2).uniqueName);
        assertSame(table.getEntry(2), table.getEntry(3));

        table.popScope();
        checker.resolveVariableUse(new Node(4, "#x"), table);
        assertSame(table.getEntry(1), table.getEntry(4));
    }

    @Test
    void variablesInOneSiblingFunctionAreNotVisibleInAnother() {
        table.pushScope(1);
        checker.declareVariable(new Node(1, "#local"), table);
        table.popScope();
        table.pushScope(1);

        expectError(2, () -> checker.resolveVariableUse(new Node(2, "#local"), table));
        assertNull(table.getEntry(2));
    }

    @Test
    void rejectsAnUndeclaredUseAtTheUseNode() {
        expectError(9, () -> checker.resolveVariableUse(new Node(9, "#missing"), table));
        assertNull(table.getEntry(9));
    }

    @Test
    void rejectsADuplicateVariableWithoutReplacingOrRenamingTheFirst() {
        checker.declareVariable(new Node(1, "#x"), table);

        SemanticException error = expectError(
                2,
                () -> checker.declareVariable(new Node(2, "#x"), table)
        );
        assertEquals("Duplicate variable declaration", error.getDescription());

        assertSame(table.getEntry(1), table.lookupVar("#x"));
        assertNull(table.getEntry(2));
        checker.declareVariable(new Node(3, "#y"), table);
        assertEquals("v2", table.getEntry(3).uniqueName);
    }

    @Test
    void reportsLocalMaskingOfAParameterSeparatelyFromDuplicates() {
        table.pushScope(1);
        checker.declareParameter(new Node(1, "#x"), table);

        SemanticException error = expectError(
                2,
                () -> checker.declareVariable(new Node(2, "#x"), table)
        );

        assertEquals("Invalid local variable name", error.getDescription());
        assertTrue(error.getMessage().toLowerCase(Locale.ROOT).contains("parameter"));
        assertFalse(error.getMessage().toLowerCase(Locale.ROOT).contains("duplicate variable"));
        assertSame(table.getEntry(1), table.lookupVar("#x"));
        assertNull(table.getEntry(2));
        checker.declareVariable(new Node(3, "#y"), table);
        assertEquals("v2", table.getEntry(3).uniqueName);
    }

    @Test
    void rejectsAVariableWithTheSameNameAsAFunctionInThisScope() {
        SymbolEntry function = new SymbolEntry("#helper", SymbolEntry.Kind.FUNC, 0, 1, "f1");
        table.currentScope().putFunc("#helper", function);

        SemanticException error = expectError(
                2,
                () -> checker.declareVariable(new Node(2, "#helper"), table)
        );

        assertEquals("Name conflict", error.getDescription());
        assertTrue(error.getMessage().toLowerCase(Locale.ROOT).contains("function"));
        assertSame(function, table.lookupFunc("#helper"));
        assertNull(table.getEntry(2));
        checker.declareVariable(new Node(3, "#other"), table);
        assertEquals("v1", table.getEntry(3).uniqueName);
    }

    @Test
    void rejectsRepeatedParameterNamesBeforeConsumingAnotherName() {
        table.pushScope(1);
        checker.declareParameter(new Node(1, "#arg"), table);

        SemanticException error = expectError(
                2,
                () -> checker.declareParameter(new Node(2, "#arg"), table)
        );
        assertEquals("Duplicate parameter declaration", error.getDescription());

        assertSame(table.getEntry(1), table.lookupVar("#arg"));
        assertNull(table.getEntry(2));
        checker.declareParameter(new Node(3, "#next"), table);
        assertEquals("v2", table.getEntry(3).uniqueName);
    }

    @Test
    void siblingDeclarationsHaveDifferentNamesAndDoNotResolveSideways() {
        table.pushScope(1);
        checker.declareVariable(new Node(1, "#x"), table);
        table.popScope();

        table.pushScope(1);
        checker.declareVariable(new Node(2, "#x"), table);
        checker.resolveVariableUse(new Node(3, "#x"), table);

        assertEquals("v1", table.getEntry(1).uniqueName);
        assertEquals("v2", table.getEntry(2).uniqueName);
        assertSame(table.getEntry(2), table.getEntry(3));
    }

    @Test
    void descendantsSeeTheirAncestorsVariableButNotASiblingVariable() {
        checker.declareVariable(new Node(1, "#x"), table);

        table.pushScope(1); // g
        checker.declareVariable(new Node(2, "#x"), table);
        table.pushScope(2); // j, inside g
        checker.resolveVariableUse(new Node(3, "#x"), table);
        assertSame(table.getEntry(2), table.getEntry(3));
        table.popScope();
        table.popScope();

        table.pushScope(1); // h, sibling of g
        table.pushScope(2); // k, inside h
        checker.resolveVariableUse(new Node(4, "#x"), table);
        assertSame(table.getEntry(1), table.getEntry(4));
        assertNotSame(table.getEntry(2), table.getEntry(4));
    }

    private static SemanticException expectError(int nodeId, Executable action) {
        SemanticException error = assertThrows(SemanticException.class, action);
        assertEquals(nodeId, error.getNodeId());
        assertFalse(error.getMessage().isBlank());
        return error;
    }

}
