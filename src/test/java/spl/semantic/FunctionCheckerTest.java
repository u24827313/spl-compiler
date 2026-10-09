package spl.semantic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spl.tree.Node;

import static org.junit.jupiter.api.Assertions.*;

class FunctionCheckerTest {

    private FunctionChecker checker;
    private SymbolTable table;

    @BeforeEach
    void setUp() {
        checker = new FunctionChecker();
        table = new SymbolTable(new NameGenerator());
        table.pushScope(0);
    }

    @Test
    void declaresFunctionSuccessfully() {
        Node declaration = new Node(1, "#calculate");

        checker.declareFunction(declaration, table);

        SymbolEntry entry = table.lookupFunc("#calculate");

        assertNotNull(entry);
        assertEquals("#calculate", entry.originalName);
        assertEquals(SymbolEntry.Kind.FUNC, entry.kind);
        assertEquals(0, entry.level);
        assertEquals(1, entry.declNodeId);
        assertEquals("f1", entry.uniqueName);

        assertSame(entry, table.getEntry(1));
    }

    @Test
    void generatesUniqueNamesForDifferentFunctions() {
        Node first = new Node(1, "#calculate");
        Node second = new Node(2, "#display");

        checker.declareFunction(first, table);
        checker.declareFunction(second, table);

        assertEquals("f1", table.lookupFunc("#calculate").uniqueName);
        assertEquals("f2", table.lookupFunc("#display").uniqueName);
    }

    @Test
    void rejectsDuplicateFunctionDeclarations() {
        Node first = new Node(1, "#calculate");
        Node duplicate = new Node(2, "#calculate");

        checker.declareFunction(first, table);

        SemanticException exception = assertThrows(
            SemanticException.class,
            () -> checker.declareFunction(duplicate, table)
        );

        assertEquals(2, exception.getNodeId());
        assertTrue(exception.getMessage().contains("Duplicate function"));
    }

    @Test
    void rejectsFunctionVariableNameClash() {
        Node variable = new Node(1, "#calculate");
        Node function = new Node(2, "#calculate");

        SymbolEntry variableEntry = new SymbolEntry(
            "#calculate",
            SymbolEntry.Kind.VAR,
            0,
            1,
            "v1"
        );

        table.currentScope().putVar("#calculate", variableEntry);

        SemanticException exception = assertThrows(
            SemanticException.class,
            () -> checker.declareFunction(function, table)
        );

        assertEquals(2, exception.getNodeId());
        assertTrue(exception.getMessage().contains("clashes with variable"));
    }

    @Test
    void resolvesDeclaredFunctionCall() {
        Node declaration = new Node(1, "#calculate");
        Node call = new Node(2, "#calculate");

        checker.declareFunction(declaration, table);
        checker.resolveFunctionCall(call, table);

        SymbolEntry declarationEntry = table.getEntry(1);
        SymbolEntry callEntry = table.getEntry(2);

        assertNotNull(callEntry);
        assertSame(declarationEntry, callEntry);
        assertEquals("f1", callEntry.uniqueName);
    }

    @Test
    void rejectsUndeclaredFunctionCall() {
        Node call = new Node(5, "#unknown");

        SemanticException exception = assertThrows(
            SemanticException.class,
            () -> checker.resolveFunctionCall(call, table)
        );

        assertEquals(5, exception.getNodeId());
        assertTrue(exception.getMessage().contains("not declared"));
    }

    @Test
    void rejectsFunctionCallFromDifferentScope() {
        Node declaration = new Node(1, "#calculate");
        Node call = new Node(2, "#calculate");

        checker.declareFunction(declaration, table);

        table.pushScope(1);

        SemanticException exception = assertThrows(
            SemanticException.class,
            () -> checker.resolveFunctionCall(call, table)
        );

        assertEquals(2, exception.getNodeId());

        table.popScope();
    }

    @Test
    void allowsSameFunctionNameInDifferentScopes() {
        Node outer = new Node(1, "#calculate");
        Node inner = new Node(2, "#calculate");

        checker.declareFunction(outer, table);

        table.pushScope(1);
        checker.declareFunction(inner, table);

        SymbolEntry innerEntry = table.lookupFunc("#calculate");

        assertNotNull(innerEntry);
        assertEquals("f2", innerEntry.uniqueName);
        assertEquals(1, innerEntry.level);

        table.popScope();

        SymbolEntry outerEntry = table.lookupFunc("#calculate");

        assertNotNull(outerEntry);
        assertEquals("f1", outerEntry.uniqueName);
        assertEquals(0, outerEntry.level);
    }

    @Test
    void rejectedDuplicateDoesNotConsumeUniqueName() {
        checker.declareFunction(new Node(1, "#calculate"), table);

        assertThrows(
            SemanticException.class,
            () -> checker.declareFunction(
                new Node(2, "#calculate"),
                table
            )
        );

        checker.declareFunction(new Node(3, "#display"), table);

        assertEquals("f2", table.lookupFunc("#display").uniqueName);
    }
}