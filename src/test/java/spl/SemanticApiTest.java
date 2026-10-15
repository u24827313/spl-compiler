package spl;

import org.junit.jupiter.api.Test;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolEntry;
import spl.semantic.SymbolTable;
import spl.tree.Node;
import spl.tree.SyntaxTree;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SemanticApiTest {

    @Test
    void laterPhaseCanReadTheUniqueNameOfAParsedDeclarationAndItsUse() {
        SyntaxTree tree = new Parser().parse(new Lexer().tokenize("#x : : #x = 5 ; $ "));
        SymbolTable symbols = new SemanticAnalyser().analyse(tree);
        List<Node> occurrences = names(tree.getRoot(), "#x");

        assertEquals(2, occurrences.size());
        SymbolEntry declaration = symbols.getEntry(occurrences.get(0).getId());
        SymbolEntry use = symbols.getEntry(occurrences.get(1).getId());
        assertSame(declaration, use);
        assertEquals("#x", declaration.getOriginalName());
        assertEquals(SymbolEntry.Kind.VAR, declaration.getKind());
        assertEquals(0, declaration.getLevel());
        assertEquals(occurrences.get(0).getId(), declaration.getDeclNodeId());
        assertEquals("v1", declaration.getUniqueName());
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
}
