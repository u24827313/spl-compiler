package spl.typeanalysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spl.tree.Node;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MixedNumberPreCheckTest {
    private TypeContext ctx;
    private MixedNumberPreCheck preCheck;
    private int nextId;

    @BeforeEach
    void setUp() {
        ctx = new TypeContext();
        preCheck = new MixedNumberPreCheck(ctx);
        nextId = 0;
    }

    @Test
    void acceptsTreeWithoutMixedNumberFeatures() {
        assertDoesNotThrow(() -> preCheck.run(node("ROOT", node("5"), node("+"))));
    }

    @Test
    void acceptsIntegerModExpression() {
        assertDoesNotThrow(() -> preCheck.run(binary("mod", "5", "2")));
    }

    @Test
    void acceptsDivWithoutMod() {
        assertDoesNotThrow(() -> preCheck.run(binary("div", "8", "2")));
    }

    @Test
    void acceptsDecimalNumberWithoutMod() {
        assertDoesNotThrow(() -> preCheck.run(node("ROOT", node("1.5"))));
    }

    @Test
    void acceptsDivWithDecimalNumber() {
        assertDoesNotThrow(() -> preCheck.run(binary("div", "8.5", "2.5")));
    }

    @Test
    void ignoresDottedStringAlongsideMod() {
        Node root = node("ROOT", binary("mod", "5", "2"), node("\"hello.world\""));

        assertDoesNotThrow(() -> preCheck.run(root));
    }

    @Test
    void quotedModAlongsideDivDoesNotCreateConflict() {
        Node root = node("ROOT", node("\"mod\""), binary("div", "8", "2"));

        assertDoesNotThrow(() -> preCheck.run(root));
    }

    @Test
    void quotedDivAlongsideModDoesNotCreateConflict() {
        Node root = node("ROOT", node("\"div\""), binary("mod", "8", "2"));

        assertDoesNotThrow(() -> preCheck.run(root));
    }

    @Test
    void rejectsModAndDivAnywhereInTreeWithFixedMessageOnRoot() {
        Node root = node("ROOT", binary("mod", "5", "2"), binary("div", "8", "2"));

        TypeException error = assertThrows(TypeException.class, () -> preCheck.run(root));

        assertEquals(MixedNumberPreCheck.CONFLICT_MESSAGE, error.getMessage());
        assertSame(root, error.getNode());
    }

    @Test
    void rejectsModAndDivInSeparateSubtrees() {
        Node modSubtree = node("FUNCTION", binary("mod", "5", "2"));
        Node divSubtree = node("INSTRUCTION", binary("div", "8", "2"));
        Node root = node("ROOT", modSubtree, divSubtree);

        TypeException error = assertThrows(TypeException.class, () -> preCheck.run(root));

        assertEquals(MixedNumberPreCheck.CONFLICT_MESSAGE, error.getMessage());
        assertSame(root, error.getNode());
    }

    @Test
    void rejectsModAndDecimalInOneExpression() {
        Node decimal = node("1.5");
        Node expression = node("TERM",
                node("mod"),
                node("("),
                node("TERM", node("5")),
                node("TERM", decimal),
                node(")"));
        Node root = node("ROOT", expression);

        TypeException error = assertThrows(TypeException.class, () -> preCheck.run(root));

        assertEquals(MixedNumberPreCheck.DECIMAL_WITH_MOD_MESSAGE, error.getMessage());
        assertSame(decimal, error.getNode());
    }

    @Test
    void rejectsDecimalEncounteredBeforeMod() {
        Node decimal = node("1.5");
        Node root = node("ROOT", decimal, binary("mod", "5", "2"));

        TypeException error = assertThrows(TypeException.class, () -> preCheck.run(root));

        assertEquals(MixedNumberPreCheck.DECIMAL_WITH_MOD_MESSAGE, error.getMessage());
        assertSame(decimal, error.getNode());
    }

    @Test
    void rejectsDecimalEncounteredAfterMod() {
        Node decimal = node("1.5");
        Node root = node("ROOT", binary("mod", "5", "2"), decimal);

        TypeException error = assertThrows(TypeException.class, () -> preCheck.run(root));

        assertEquals(MixedNumberPreCheck.DECIMAL_WITH_MOD_MESSAGE, error.getMessage());
        assertSame(decimal, error.getNode());
    }

    @Test
    void rejectsNegativeDecimalAlongsideMod() {
        Node decimal = node("-1.5");
        Node root = node("ROOT", binary("mod", "5", "2"), decimal);

        TypeException error = assertThrows(TypeException.class, () -> preCheck.run(root));

        assertEquals(MixedNumberPreCheck.DECIMAL_WITH_MOD_MESSAGE, error.getMessage());
        assertSame(decimal, error.getNode());
    }

    @Test
    void reportsFirstDecimalWhenSeveralOccurWithMod() {
        Node firstDecimal = node("1.5");
        Node secondDecimal = node("2.5");
        Node root = node("ROOT", firstDecimal, secondDecimal, binary("mod", "5", "2"));

        TypeException error = assertThrows(TypeException.class, () -> preCheck.run(root));

        assertEquals(MixedNumberPreCheck.DECIMAL_WITH_MOD_MESSAGE, error.getMessage());
        assertSame(firstDecimal, error.getNode());
    }

    @Test
    void modDivConflictTakesPrecedenceOverDecimalConflict() {
        Node decimal = node("1.5");
        Node root = node("ROOT", decimal, binary("mod", "5", "2"), binary("div", "8", "2"));

        TypeException error = assertThrows(TypeException.class, () -> preCheck.run(root));

        assertEquals(MixedNumberPreCheck.CONFLICT_MESSAGE, error.getMessage());
        assertSame(root, error.getNode());
    }

    @Test
    void resetsStateAfterFailingScan() {
        Node conflictingTree = node("ROOT", binary("mod", "5", "2"), binary("div", "8", "2"));
        assertThrows(TypeException.class, () -> preCheck.run(conflictingTree));

        assertDoesNotThrow(() -> preCheck.run(node("ROOT", node("5"))));
    }

    @Test
    void resetsStateAfterSuccessfulModScan() {
        assertDoesNotThrow(() -> preCheck.run(binary("mod", "5", "2")));

        assertDoesNotThrow(() -> preCheck.run(node("ROOT", node("1.5"))));
    }

    @Test
    void resetsStateAfterSuccessfulDivScan() {
        assertDoesNotThrow(() -> preCheck.run(binary("div", "8", "2")));

        assertDoesNotThrow(() -> preCheck.run(binary("mod", "5", "2")));
    }

    @Test
    void doesNotChangeNodeOrNameTypes() {
        Node name = node("#x");
        Node decimal = node("1.5");
        Node root = node("ROOT", name, decimal);
        ctx.setNameType("#x", Type.NUMERIC);

        assertDoesNotThrow(() -> preCheck.run(root));

        assertEquals(Type.UNKNOWN, ctx.typeOf(root));
        assertEquals(Type.UNKNOWN, ctx.typeOf(name));
        assertEquals(Type.UNKNOWN, ctx.typeOf(decimal));
        assertEquals(Type.NUMERIC, ctx.nameType("#x"));
    }

    private Node binary(String operator, String left, String right) {
        return node("TERM",
                node(operator),
                node("("),
                node("TERM", node(left)),
                node("TERM", node(right)),
                node(")"));
    }

    private Node node(String contents, Node... children) {
        Node result = new Node(nextId++, contents);
        for (Node child : children) {
            result.addChild(child);
        }
        return result;
    }
}
