package spl.semantic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.tree.Node;
import spl.tree.SyntaxTree;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticPipelineIntegrationTest {

    private record Result(SyntaxTree tree, SymbolTable symbols) {}

    @Test
    void acceptsEmptyDeclarationAndInstructionChains() {
        Result result = analyse(": : $ ");

        assertEquals("SPL_PROG", result.tree().getRoot().getContents());
        assertTrue(names(result.tree().getRoot(), "#anything").isEmpty());
    }

    @Test
    void declarationsAndUsesInRealParsedInstructionsShareEntries() {
        Result result = analyse("#x #y : : #x = 5 ; #y = #x ; $ ");
        List<Node> xs = names(result.tree().getRoot(), "#x");
        List<Node> ys = names(result.tree().getRoot(), "#y");

        assertEquals(3, xs.size());
        assertEquals(2, ys.size());
        assertSame(entry(result, xs.get(0)), entry(result, xs.get(1)));
        assertSame(entry(result, xs.get(0)), entry(result, xs.get(2)));
        assertSame(entry(result, ys.get(0)), entry(result, ys.get(1)));
        assertNotSame(entry(result, xs.get(0)), entry(result, ys.get(0)));
        assertEquals(SymbolEntry.Kind.VAR, entry(result, xs.get(0)).kind);
        assertEquals(0, entry(result, xs.get(0)).level);
        assertAllNamesRecorded(result);
    }

    @Test
    void numReturnTermResolvesParameterUsesInsideTheFunctionBodyScope() {
        Result result = analyse(": num #square ( #n ) { : : return ( mul ( #n #n ) ) } : $ ");
        List<Node> ns = names(result.tree().getRoot(), "#n");
        Node square = names(result.tree().getRoot(), "#square").get(0);

        assertEquals(3, ns.size());
        assertEquals(SymbolEntry.Kind.PARAM, entry(result, ns.get(0)).kind);
        assertEquals(1, entry(result, ns.get(0)).level);
        assertSame(entry(result, ns.get(0)), entry(result, ns.get(1)));
        assertSame(entry(result, ns.get(0)), entry(result, ns.get(2)));
        assertEquals(SymbolEntry.Kind.FUNC, entry(result, square).kind);
        assertEquals(0, entry(result, square).level);
        assertAllNamesRecorded(result);
    }

    @Test
    void voidFunctionAndCallResolveToTheSameDeclaration() {
        Result result = analyse(": void #greet ( #who ) { : : print ( #who ) ; return } : #greet ( 7 ) ; $ ");
        List<Node> greets = names(result.tree().getRoot(), "#greet");
        List<Node> whos = names(result.tree().getRoot(), "#who");

        assertEquals(2, greets.size());
        assertEquals(2, whos.size());
        assertSame(entry(result, greets.get(0)), entry(result, greets.get(1)));
        assertSame(entry(result, whos.get(0)), entry(result, whos.get(1)));
        assertAllNamesRecorded(result);
    }

    @Test
    void functionCallsCanUseDeclarationsLaterInTheSameChain() {
        Result result = analyse(": void #first ( ) { : : return } void #second ( ) { : : return } : #second ( ) ; #first ( ) ; $ ");
        for (String name : List.of("#first", "#second")) {
            List<Node> occurrences = names(result.tree().getRoot(), name);
            assertEquals(2, occurrences.size());
            assertSame(entry(result, occurrences.get(0)), entry(result, occurrences.get(1)));
        }
        assertNotSame(entry(result, names(result.tree().getRoot(), "#first").get(0)),
                entry(result, names(result.tree().getRoot(), "#second").get(0)));
    }

    @Test
    void nearestVariableWinsAndOuterVariableReturnsAfterFunctionScope() {
        Result result = analyse("#x : void #f ( ) { #x : : print ( #x ) ; return } : print ( #x ) ; $ ");
        List<Node> xs = names(result.tree().getRoot(), "#x");

        assertEquals(4, xs.size());
        SymbolEntry outer = entry(result, xs.get(0));
        SymbolEntry local = entry(result, xs.get(1));
        assertNotSame(outer, local);
        assertNotEquals(outer.uniqueName, local.uniqueName);
        assertSame(local, entry(result, xs.get(2)));
        assertSame(outer, entry(result, xs.get(3)));
    }

    @Test
    void nestedHelpersCanReuseNamesInDifferentParentFunctions() {
        Result result = analyse(": void #f ( ) { : void #h ( ) { : : return } : #h ( ) ; return } "
                + "void #g ( ) { : void #h ( ) { : : return } : #h ( ) ; return } : $ ");
        List<Node> hs = names(result.tree().getRoot(), "#h");

        assertEquals(4, hs.size());
        assertSame(entry(result, hs.get(0)), entry(result, hs.get(1)));
        assertSame(entry(result, hs.get(2)), entry(result, hs.get(3)));
        assertNotSame(entry(result, hs.get(0)), entry(result, hs.get(2)));
        assertEquals(1, entry(result, hs.get(0)).level);
        assertEquals(1, entry(result, hs.get(2)).level);
        assertAllNamesRecorded(result);
    }

    @Test
    void descendantsSeeAncestorsButNotVariablesFromSiblingFunctions() {
        Result result = analyse("#x : void #g ( ) { #x : void #j ( ) { : : print ( #x ) ; return } : return } "
                + "void #h ( ) { : void #k ( ) { : : print ( #x ) ; return } : return } : $ ");
        List<Node> xs = names(result.tree().getRoot(), "#x");

        assertEquals(4, xs.size());
        assertSame(entry(result, xs.get(1)), entry(result, xs.get(2)));
        assertSame(entry(result, xs.get(0)), entry(result, xs.get(3)));
        assertNotSame(entry(result, xs.get(1)), entry(result, xs.get(3)));
    }

    @Test
    void usesInBranchAndLoopBodiesStayInTheCurrentScope() {
        Result result = analyse("#x : : if eq ( #x 0 ) then { #x = add ( #x 1 ) ; } "
                + "else { print ( #x ) ; } ; while eq ( #x 0 ) do { #x = 1 ; } ; $ ");
        List<Node> xs = names(result.tree().getRoot(), "#x");

        assertTrue(xs.size() > 4);
        SymbolEntry declaration = entry(result, xs.get(0));
        for (Node use : xs.subList(1, xs.size())) {
            assertSame(declaration, entry(result, use));
        }
        assertAllNamesRecorded(result);
    }

    @Test
    void nestedCallsAndTheirArgumentsAreAllRecorded() {
        Result result = analyse("#x : num #sq ( #n ) { : : return ( mul ( #n #n ) ) } "
                + ": #x = #sq ( #sq ( 2 ) ) ; $ ");
        List<Node> squares = names(result.tree().getRoot(), "#sq");

        assertEquals(3, squares.size());
        assertSame(entry(result, squares.get(0)), entry(result, squares.get(1)));
        assertSame(entry(result, squares.get(0)), entry(result, squares.get(2)));
        assertAllNamesRecorded(result);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPrograms")
    void rejectsInvalidProgramsAtTheOffendingName(String rule, String source, String name, String parentKind) {
        SyntaxTree tree = parse(source);
        SemanticException error = assertThrows(SemanticException.class,
                () -> new SemanticAnalyzer().analyse(tree.getRoot()), rule);
        Node offender = findById(tree.getRoot(), error.getNodeId());

        assertNotNull(offender, "Error must identify a node in the parsed tree: " + rule);
        assertEquals(name, offender.getContents(), rule);
        assertEquals(parentKind, offender.getParent().getContents(), rule);
    }

    private static Stream<Arguments> invalidPrograms() {
        return Stream.of(
                Arguments.of("undeclared variable", ": : print ( #missing ) ; $ ", "#missing", "TERM"),
                Arguments.of("undeclared function", ": : #missing ( ) ; $ ", "#missing", "CALL"),
                Arguments.of("duplicate variable", "#x #x : : $ ", "#x", "V_DECL"),
                Arguments.of("duplicate function", ": void #f ( ) { : : return } void #f ( ) { : : return } : $ ", "#f", "F_TYPE"),
                Arguments.of("duplicate parameter", ": void #f ( #x #x ) { : : return } : $ ", "#x", "V_DECL"),
                Arguments.of("local masks parameter", ": void #f ( #x ) { #x : : return } : $ ", "#x", "V_DECL"),
                Arguments.of("variable and function clash", "#f : void #f ( ) { : : return } : $ ", "#f", "F_TYPE"),
                Arguments.of("direct recursion", ": void #f ( ) { : : #f ( ) ; return } : $ ", "#f", "CALL"),
                Arguments.of("indirect recursion", ": void #f ( ) { : : #g ( ) ; return } "
                        + "void #g ( ) { : : #f ( ) ; return } : $ ", "#g", "CALL"),
                Arguments.of("outer-level function call", ": void #helper ( ) { : : return } "
                        + "void #caller ( ) { : : #helper ( ) ; return } : $ ", "#helper", "CALL"),
                Arguments.of("sideways variable use", ": void #g ( ) { #x : : return } "
                        + "void #h ( ) { : : print ( #x ) ; return } : $ ", "#x", "TERM")
        );
    }

    private static SyntaxTree parse(String source) {
        return new Parser().parse(new Lexer().tokenize(source));
    }

    private static Result analyse(String source) {
        SyntaxTree tree = parse(source);
        return new Result(tree, new SemanticAnalyzer().analyse(tree.getRoot()));
    }

    private static SymbolEntry entry(Result result, Node node) {
        SymbolEntry entry = result.symbols().getEntry(node.getId());
        assertNotNull(entry, "Missing symbol entry for " + node.getContents() + " at node " + node.getId());
        return entry;
    }

    private static List<Node> names(Node root, String name) {
        List<Node> matches = new ArrayList<>();
        collectNames(root, name, matches);
        return matches;
    }

    private static void collectNames(Node node, String name, List<Node> matches) {
        if (node.isLeaf() && node.getContents().equals(name)) {
            matches.add(node);
        }
        for (Node child : node.getChildren()) {
            collectNames(child, name, matches);
        }
    }

    private static void assertAllNamesRecorded(Result result) {
        assertAllNamesRecorded(result.tree().getRoot(), result);
    }

    private static void assertAllNamesRecorded(Node node, Result result) {
        if (NodeKinds.isUserDefinedName(node)) {
            entry(result, node);
        }
        for (Node child : node.getChildren()) {
            assertAllNamesRecorded(child, result);
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
