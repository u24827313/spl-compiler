package spl.semantic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.tree.Node;
import spl.tree.SyntaxTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticFixtureProgramsTest {

    private record Result(SyntaxTree tree, SymbolTable symbols) {}

    @ParameterizedTest(name = "valid: {0}")
    @ValueSource(strings = {
            "shadowed-variable.spl.txt",
            "copied-helper.spl.txt",
            "multi-level-nested-functions.spl.txt",
            "num-return-parameter.spl.txt",
            "void-function.spl.txt",
            "empty-declaration-chains.spl.txt",
            "nested-if-while-names-and-calls.spl.txt",
            "sibling-functions-each-declare-x.spl.txt",
            "ancestor-variable-a21.spl.txt"
    })
    void validProgramsRecordEveryName(String fixture) throws IOException {
        Result result = analyse(fixture);
        assertAllNamesRecorded(result.tree().getRoot(), result.symbols(), fixture);
    }

    @ParameterizedTest(name = "invalid: {0}")
    @MethodSource("invalidPrograms")
    void invalidProgramsIdentifyAnOffendingName(String fixture, String name, String parentKind) throws IOException {
        SyntaxTree tree = parse("invalid", fixture);
        SemanticException error = assertThrows(SemanticException.class,
                () -> new SemanticAnalyzer().analyse(tree.getRoot()), fixture);
        Node offender = findById(tree.getRoot(), error.getNodeId());

        assertNotNull(offender, fixture + ": error node must be in the parsed tree");
        assertEquals(name, offender.getContents(), fixture);
        assertEquals(parentKind, offender.getParent().getContents(), fixture);
        assertTrue(error.getMessage().contains("node " + error.getNodeId()), fixture);
    }

    private static Stream<Arguments> invalidPrograms() {
        return Stream.of(
                Arguments.of("undeclared-variable.spl.txt", "#missing", "TERM"),
                Arguments.of("undeclared-function.spl.txt", "#missing", "CALL"),
                Arguments.of("duplicate-variable.spl.txt", "#x", "V_DECL"),
                Arguments.of("duplicate-function.spl.txt", "#f", "F_TYPE"),
                Arguments.of("local-masks-parameter.spl.txt", "#x", "V_DECL"),
                Arguments.of("wrong-level-call.spl.txt", "#helper", "CALL"),
                Arguments.of("direct-recursion.spl.txt", "#f", "CALL"),
                Arguments.of("indirect-recursion.spl.txt", "#g", "CALL"),
                Arguments.of("variable-function-name-clash.spl.txt", "#f", "F_TYPE"),
                Arguments.of("sideways-sibling-variable.spl.txt", "#x", "TERM")
        );
    }

    @Test
    void siblingLocalsAreDistinctAndDescendantsUseOnlyTheirOwnAncestors() throws IOException {
        Result siblings = analyse("sibling-functions-each-declare-x.spl.txt");
        List<Node> xs = names(siblings.tree().getRoot(), "#x");
        assertEquals(4, xs.size());
        assertSame(entry(siblings, xs.get(0)), entry(siblings, xs.get(1)));
        assertSame(entry(siblings, xs.get(2)), entry(siblings, xs.get(3)));
        assertNotSame(entry(siblings, xs.get(0)), entry(siblings, xs.get(2)));

        Result ancestors = analyse("ancestor-variable-a21.spl.txt");
        xs = names(ancestors.tree().getRoot(), "#x");
        assertEquals(4, xs.size());
        assertSame(entry(ancestors, xs.get(1)), entry(ancestors, xs.get(2)));
        assertSame(entry(ancestors, xs.get(0)), entry(ancestors, xs.get(3)));
        assertNotSame(entry(ancestors, xs.get(1)), entry(ancestors, xs.get(3)));
    }

    private static Result analyse(String fixture) throws IOException {
        SyntaxTree tree = parse("valid", fixture);
        return new Result(tree, new SemanticAnalyzer().analyse(tree.getRoot()));
    }

    private static SyntaxTree parse(String category, String fixture) throws IOException {
        Path path = Path.of("tests", "resources", "semantic", category, fixture);
        return new Parser().parse(new Lexer().tokenize(Files.readString(path)));
    }

    private static void assertAllNamesRecorded(Node node, SymbolTable symbols, String fixture) {
        if (NodeKinds.isUserDefinedName(node)) {
            assertNotNull(symbols.getEntry(node.getId()),
                    fixture + ": missing entry for " + node.getContents() + " at node " + node.getId());
        }
        for (Node child : node.getChildren()) {
            assertAllNamesRecorded(child, symbols, fixture);
        }
    }

    private static SymbolEntry entry(Result result, Node node) {
        SymbolEntry entry = result.symbols().getEntry(node.getId());
        assertNotNull(entry, "Missing entry for " + node.getContents());
        return entry;
    }

    private static List<Node> names(Node root, String name) {
        List<Node> result = new ArrayList<>();
        collectNames(root, name, result);
        return result;
    }

    private static void collectNames(Node node, String name, List<Node> matches) {
        if (node.isLeaf() && node.getContents().equals(name)) {
            matches.add(node);
        }
        for (Node child : node.getChildren()) {
            collectNames(child, name, matches);
        }
    }

    private static Node findById(Node node, int id) {
        if (node.getId() == id) {
            return node;
        }
        for (Node child : node.getChildren()) {
            Node match = findById(child, id);
            if (match != null) {
                return match;
            }
        }
        return null;
    }
}
