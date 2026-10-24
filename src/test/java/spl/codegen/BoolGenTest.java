package spl.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.io.TempDir;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolTable;
import spl.tree.Node;
import spl.typeanalysis.TypeAnalyser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BoolGenTest {
    @TempDir Path tempDir;
    private int sequence;

    @TestFactory
    Stream<DynamicTest> comparisons() {
        List<Case> cases = new ArrayList<>();
        for (String op : List.of("eq", "larger", "lesser")) {
            for (int left : new int[]{-2, 0, 3}) {
                for (int right : new int[]{-2, 0, 3}) {
                    boolean expected = op.equals("eq") ? left == right
                            : op.equals("larger") ? left > right : left < right;
                    cases.add(new Case(op + " ( " + left + " " + right + " )", expected));
                }
            }
        }
        cases.add(new Case("eq ( add ( 2 mul ( 3 4 ) ) 14 )", true));
        cases.add(new Case("lesser ( neg ( 3 ) sub ( 1 2 ) )", true));
        cases.add(new Case("eq ( mod ( 8 3 ) 2 )", true));
        cases.add(new Case("eq ( div ( 5 2 ) 2.5 )", true, "float"));
        cases.add(new Case("larger ( -1.5 -2.5 )", true, "float"));
        cases.add(new Case("lesser ( 1.5 1.5 )", false, "float"));
        cases.add(new Case("eq ( #x add ( #y 1 ) )", true));
        return tests(cases);
    }

    @TestFactory
    Stream<DynamicTest> exhaustiveTruthTablesAndNestedCompositions() {
        List<Case> cases = new ArrayList<>();
        for (boolean a : new boolean[]{false, true}) {
            cases.add(new Case("not ( " + literal(a) + " )", !a));
            cases.add(new Case("not ( not ( " + literal(a) + " ) )", a));
            for (boolean b : new boolean[]{false, true}) {
                String x = literal(a), y = literal(b);
                cases.add(new Case("or ( " + x + " " + y + " )", a || b));
                cases.add(new Case("and ( " + x + " " + y + " )", a && b));
                cases.add(new Case("not ( or ( " + x + " " + y + " ) )", !(a || b)));
                cases.add(new Case("not ( and ( " + x + " " + y + " ) )", !(a && b)));
                for (boolean c : new boolean[]{false, true}) {
                    String z = literal(c);
                    cases.add(new Case("or ( and ( " + x + " " + y + " ) " + z + " )", (a && b) || c));
                    cases.add(new Case("and ( or ( " + x + " " + y + " ) " + z + " )", (a || b) && c));
                    cases.add(new Case("and ( " + x + " or ( " + y + " " + z + " ) )", a && (b || c)));
                    cases.add(new Case("or ( " + x + " and ( " + y + " " + z + " ) )", a || (b && c)));
                }
            }
        }
        return tests(cases);
    }

    @TestFactory
    Stream<DynamicTest> deterministicDeepTreesAgainstIndependentBooleanOracle() {
        Random random = new Random(341);
        List<Case> cases = new ArrayList<>();
        for (int i = 0; i < 24; i++) cases.add(randomExpression(random, 5));
        return tests(cases);
    }

    @TestFactory
    Stream<DynamicTest> shortCircuitSkipsInvalidArithmetic() {
        String trap = "eq ( mod ( 1 #zero ) 0 )";
        return tests(List.of(
                new Case("or ( " + literal(true) + " " + trap + " )", true),
                new Case("and ( " + literal(false) + " " + trap + " )", false),
                new Case("not ( or ( " + literal(true) + " " + trap + " ) )", false),
                new Case("not ( and ( " + literal(false) + " " + trap + " ) )", true)));
    }

    @Test
    void comparisonEmitsExpectedOperatorAndRegistersTemporaries() {
        for (String op : List.of("eq", "larger", "lesser")) {
            Fixture f = fixture(new Case(op + " ( 2 3 )", false));
            String symbol = op.equals("eq") ? "==" : op.equals("larger") ? ">" : "<";
            assertEquals("t1 = 2;\nt2 = 3;\nif ( t1 " + symbol + " t2 ) goto YES;\n",
                    BoolGen.transBool(f.bool(), "YES", "UNUSED", f.ctx()));
            assertEquals("int t1;\nint t2;\n", f.ctx().declarations());
        }
    }

    @Test
    void repeatedTranslationsShareFreshLabelsAndTemporaries() throws Exception {
        Fixture f = fixture(new Case("and ( not ( eq ( #x 0 ) ) lesser ( #y #x ) )", true));
        String first = f.ctx().newLabel(), second = f.ctx().newLabel();
        String body = BoolGen.transBool(f.bool(), first, "UNUSED", f.ctx())
                + "return 10;\n" + first + ": ;\n"
                + BoolGen.transBool(f.bool(), second, "UNUSED", f.ctx())
                + "return 11;\n" + second + ": ;\nreturn 1;\n";
        assertEquals(1, execute(f, body));
    }

    @Test
    void directTransNotUsedByLoopsPreservesContract() throws Exception {
        for (boolean value : new boolean[]{false, true}) {
            Fixture f = fixture(new Case(literal(value), value));
            String target = f.ctx().newLabel();
            String body = BoolGen.transNot(f.bool(), target, "UNUSED", f.ctx())
                    + "return 0;\n" + target + ": ;\nreturn 1;\n";
            assertEquals(value ? 0 : 1, execute(f, body));
        }
    }

    @Test
    void rejectsEmptyBooleanAndUnknownOperator() {
        CodeGenContext ctx = new CodeGenContext(null, "int");
        assertThrows(IllegalStateException.class,
                () -> BoolGen.transBool(new Node(90001, "BOOL"), "YES", "NO", ctx));
        Node unknown = new Node(90002, "BOOL");
        unknown.addChild(new Node(90003, "xor"));
        assertThrows(IllegalArgumentException.class,
                () -> BoolGen.transBool(unknown, "YES", "NO", ctx));
        assertThrows(IllegalArgumentException.class, () -> BoolGen.opSymbol("xor"));
    }

    @Test
    void supportsFunctionCallInComparison() {
        var tree = new Parser().parse(new Lexer().tokenize(
                ": num #f ( ) { : : return ( 1 ) } : "
                + "if eq ( #f ( ) 1 ) then { } else { } ; $ "));
        SymbolTable symbols = new SemanticAnalyser().analyse(tree);
        new TypeAnalyser(symbols).analyse(tree.getRoot());
        FunctionRegistry registry =
                FunctionRegistry.build(tree.getRoot(), symbols);
        CodeGenContext ctx =
                new CodeGenContext(symbols, "int", registry);

        assertDoesNotThrow(() -> BoolGen.transBool(
                find(tree.getRoot(), "BOOL"), "YES", "NO", ctx));
    }

    private Stream<DynamicTest> tests(List<Case> cases) {
        return cases.stream().map(c -> DynamicTest.dynamicTest(c.expression() + " -> " + c.expected(), () -> {
            Fixture f = fixture(c);
            String target = f.ctx().newLabel();
            String code = BoolGen.transBool(f.bool(), target, "UNUSED", f.ctx());
            assertFalse(code.matches("(?s).*\\belse\\b.*"), code);
            assertFalse(code.contains("&&") || code.contains("||") || code.contains("!"), code);
            assertEquals(c.expected() ? 1 : 0, execute(f,
                    code + "return 0;\n" + target + ": ;\nreturn 1;\n"), c.expression());
        }));
    }

    private Fixture fixture(Case c) {
        var tree = new Parser().parse(new Lexer().tokenize(
                "#x #y #zero : : if " + c.expression() + " then { } else { } ; $ "));
        SymbolTable symbols = new SemanticAnalyser().analyse(tree);
        new TypeAnalyser(symbols).analyse(tree.getRoot());
        Node declarations = find(tree.getRoot(), "V_DECL");
        List<String> names = new ArrayList<>();
        for (Node chain = declarations; !chain.getChildren().isEmpty(); chain = chain.getChildren().get(1)) {
            names.add(symbols.getEntry(chain.getChildren().get(0).getId()).getUniqueName());
        }
        return new Fixture(find(tree.getRoot(), "BOOL"), new CodeGenContext(symbols, c.cType()),
                c.cType() + " " + names.get(0) + " = 3;\n"
                + c.cType() + " " + names.get(1) + " = 2;\n"
                + "volatile " + c.cType() + " " + names.get(2) + " = 0;\n");
    }

    private int execute(Fixture f, String body) throws Exception {
        boolean gccAvailable;
        try {
            Process probe = new ProcessBuilder("gcc", "--version").redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            gccAvailable = probe.waitFor(5, TimeUnit.SECONDS);
            if (!gccAvailable) probe.destroyForcibly();
            else gccAvailable = probe.exitValue() == 0;
        } catch (java.io.IOException e) { gccAvailable = false; }
        assumeTrue(gccAvailable, "Generated-C execution tests require gcc on PATH");
        Path source = tempDir.resolve("condition-" + (++sequence) + ".c");
        Path binary = tempDir.resolve("condition-" + sequence);
        Path log = tempDir.resolve("compiler-" + sequence + ".log");
        String program = "int main(void) {\n" + f.initializers() + f.ctx().declarations() + body + "}\n";
        Files.writeString(source, program);
        Process compiler = new ProcessBuilder("gcc", "-std=c11", "-pedantic-errors", "-O0",
                source.toString(), "-o", binary.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!compiler.waitFor(10, TimeUnit.SECONDS)) {
            compiler.destroyForcibly();
            fail("C compilation timed out: " + source);
        }
        assertEquals(0, compiler.exitValue(), () -> {
            try { return Files.readString(log) + "\n" + program; }
            catch (java.io.IOException e) { return program; }
        });
        Process execution = new ProcessBuilder(binary.toString()).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        if (!execution.waitFor(5, TimeUnit.SECONDS)) {
            execution.destroyForcibly();
            fail("Generated code did not terminate:\n" + program);
        }
        return execution.exitValue();
    }

    private static String literal(boolean value) { return "eq ( 1 " + (value ? "1" : "2") + " )"; }

    private static Case randomExpression(Random r, int depth) {
        if (depth == 0) {
            boolean value = r.nextBoolean();
            return new Case(literal(value), value);
        }
        Case left = randomExpression(r, depth - 1);
        int op = r.nextInt(3);
        if (op == 0) return new Case("not ( " + left.expression() + " )", !left.expected());
        Case right = randomExpression(r, depth - 1);
        return new Case((op == 1 ? "and" : "or") + " ( " + left.expression() + " " + right.expression() + " )",
                op == 1 ? left.expected() && right.expected() : left.expected() || right.expected());
    }

    private static Node find(Node node, String kind) {
        if (kind.equals(node.getContents())) return node;
        for (Node child : node.getChildren()) {
            Node result = find(child, kind);
            if (result != null) return result;
        }
        return null;
    }

    private record Case(String expression, boolean expected, String cType) {
        Case(String expression, boolean expected) { this(expression, expected, "int"); }
    }
    private record Fixture(Node bool, CodeGenContext ctx, String initializers) {}
}