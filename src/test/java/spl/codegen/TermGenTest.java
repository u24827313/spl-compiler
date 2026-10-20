package spl.codegen;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolTable;
import spl.tree.Node;
import spl.tree.SyntaxTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TermGenTest {

    @Test
    void translatesNameThroughItsSymbolTableEntry() {
        Fixture fixture = fixture("#x");

        assertEquals("result = v1;\n",
                TermGen.transTerm(fixture.term(), "result", fixture.context()));
    }

    @Test
    void translatesNumericLiteralWithoutChangingItsText() {
        Fixture fixture = fixture("12.5");

        assertEquals("result = 12.5;\n",
                TermGen.transTerm(fixture.term(), "result", fixture.context()));
    }

    @ParameterizedTest
    @CsvSource({"mod, %", "div, /", "add, +", "sub, -", "mul, *"})
    void translatesBinaryOperators(String operator, String targetOperator) {
        Fixture fixture = fixture(operator + " ( 8 2 )");

        String code = TermGen.transTerm(fixture.term(), "result", fixture.context());

        assertEquals("t1 = 8;\nt2 = 2;\nresult = t1 " + targetOperator + " t2;\n", code);
        assertEquals("int t1;\nint t2;\n", fixture.context().declarations());
    }

    @Test
    void translatesNegationWithADeclaredTemporary() {
        Fixture fixture = fixture("neg ( 3 )");

        assertEquals("t1 = 3;\nresult = -t1;\n",
                TermGen.transTerm(fixture.term(), "result", fixture.context()));
        assertEquals("int t1;\n", fixture.context().declarations());
    }

    @Test
    void translatesNestedExpressionsPostorder() {
        Fixture fixture = fixture("add ( mul ( 2 3 ) 4 )");

        assertEquals("t3 = 2;\nt4 = 3;\nt1 = t3 * t4;\nt2 = 4;\n"
                        + "result = t1 + t2;\n",
                TermGen.transTerm(fixture.term(), "result", fixture.context()));
        assertEquals("int t1;\nint t2;\nint t3;\nint t4;\n",
                fixture.context().declarations());
    }

    @Test
    void rejectsUnknownTermAlternativesInsteadOfEmittingThemAsLiterals() {
        Fixture fixture = fixture("1");
        Node unknown = node(90_001, "TERM", node(90_002, "pow"));

        assertThrows(IllegalArgumentException.class,
                () -> TermGen.transTerm(unknown, "result", fixture.context()));
    }

    @Test
    void leavesFunctionCallsForPhase3b() {
        Fixture fixture = fixture("1");
        Node call = node(90_003, "CALL");
        Node term = node(90_004, "TERM", call);

        assertThrows(UnsupportedOperationException.class,
                () -> TermGen.transTerm(term, "result", fixture.context()));
    }

    private static Fixture fixture(String expression) {
        SyntaxTree tree = new Parser().parse(new Lexer().tokenize(
                "#x : : #x = " + expression + " ; $ "));
        SymbolTable symbols = new SemanticAnalyser().analyse(tree);
        Node assignment = find(tree.getRoot(), "ASSIGN");
        return new Fixture(assignment.getChildren().get(2),
                new CodeGenContext(symbols, "int"));
    }

    private static Node find(Node node, String contents) {
        if (contents.equals(node.getContents())) {
            return node;
        }
        for (Node child : node.getChildren()) {
            Node found = find(child, contents);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Node node(int id, String contents, Node... children) {
        Node result = new Node(id, contents);
        for (Node child : children) {
            result.addChild(child);
        }
        return result;
    }

    private record Fixture(Node term, CodeGenContext context) {}
}