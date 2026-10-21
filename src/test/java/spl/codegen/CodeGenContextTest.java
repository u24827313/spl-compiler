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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    // ---- 3b additions ----

    @Test
    void prefixedVariableIsRegisteredForDeclaration() {
        CodeGenContext context = new CodeGenContext(analyse("#x : : #x = 5 ; $ "), "int");

        assertEquals("t1", context.newVar());
        assertEquals("f1t2", context.newVar("f1"));
        assertEquals("int t1;\nint f1t2;\n", context.declarations());
    }

    @Test
    void userVariablesAreDeclaredOnceAndBeforeTemps() {
        CodeGenContext context = new CodeGenContext(analyse("#x : : #x = 5 ; $ "), "float");

        context.newVar();
        context.declareUserVar("v1");
        context.declareUserVar("v2");
        context.declareUserVar("v1");   // duplicate is ignored

        assertEquals("float v1;\nfloat v2;\nfloat t1;\n", context.declarations());
    }

    @Test
    void scratchContextHasItsOwnCountersAndDeclarations() {
        CodeGenContext context = new CodeGenContext(analyse("#x : : #x = 5 ; $ "), "int");
        context.newVar();
        context.newLabel();

        CodeGenContext scratch = context.scratch();

        assertNotSame(context, scratch);
        assertEquals("t1", scratch.newVar());
        assertEquals("L1", scratch.newLabel());
        assertEquals("int t1;\n", context.declarations());   // untouched by the scratch var
    }

    @Test
    void registryIsRejectedWhenContextWasBuiltWithoutOne() {
        CodeGenContext context = new CodeGenContext(analyse("#x : : #x = 5 ; $ "), "int");

        assertThrows(IllegalStateException.class, context::registry);
    }

    @Test
    void inlineDepthGuardTripsOnRunawayNesting() {
        CodeGenContext context = new CodeGenContext(analyse("#x : : #x = 5 ; $ "), "int");

        for (int i = 0; i < 100; i++) {
            context.beginInline();
        }
        assertThrows(IllegalStateException.class, context::beginInline);
    }

    @Test
    void inlineDepthIsReleasedByEndInline() {
        CodeGenContext context = new CodeGenContext(analyse("#x : : #x = 5 ; $ "), "int");

        for (int i = 0; i < 1000; i++) {
            context.beginInline();
            context.endInline();
        }
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