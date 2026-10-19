package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolTable;
import spl.tree.Node;
import spl.tree.SyntaxTree;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeGenContextTest {

    @Test
    void generatesDistinctTemporaryAndLabelNamesAndDeclarations() {
        SymbolTable symbols = analyse("#x : : #x = 5 ; $ ");
        CodeGenContext context = new CodeGenContext(symbols, "int");

        assertEquals("t1", context.newVar());
        assertEquals("t2", context.newVar());
        assertEquals("L1", context.newLabel());
        assertEquals("L2", context.newLabel());
        assertEquals("int t1;\nint t2;\n", context.declarations());
    }

    @Test
    void looksUpTheInternalNameForAParsedVariableUse() {
        SyntaxTree tree = parse("#x : : #x = 5 ; $ ");
        SymbolTable symbols = new SemanticAnalyser().analyse(tree);
        Node variableUse = names(tree.getRoot(), "#x").get(1);
        CodeGenContext context = new CodeGenContext(symbols, "float");

        assertEquals("v1", context.lookUp(variableUse));
    }

    private static SymbolTable analyse(String source) {
        return new SemanticAnalyser().analyse(parse(source));
    }

    private static SyntaxTree parse(String source) {
        return new Parser().parse(new Lexer().tokenize(source));
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