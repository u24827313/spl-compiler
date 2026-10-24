package spl.codegen;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import spl.Main;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolTable;
import spl.tree.Node;
import spl.typeanalysis.TypeAnalyser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ProgramGenTest {
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(strings = {": : $ ", "#x : : #x = -2 ; $ ",
            ": : print ( mod ( 5 2 ) ) ; $ ", ": : print \"hello.world\" ; $ "})
    void choosesIntWithoutFloatingArithmetic(String source) {
        assertEquals("int", ProgramGen.detectCType(parse(source)));
    }

    @ParameterizedTest
    @ValueSource(strings = {": : print ( 1.5 ) ; $ ", ": : print ( -0.5 ) ; $ ",
            ": : print ( div ( 5 2 ) ) ; $ ",
            ": num #unused ( ) { : : return ( 0.5 ) } : $ "})
    void choosesFloatAcrossTheWholeTreeIncludingDivision(String source) {
        assertEquals("float", ProgramGen.detectCType(parse(source)));
    }

    @Test
    void hoistsGlobalsParametersAndNestedLocalsOnce() {
        Fixture f = fixture("#x : void #outer ( #a ) { #x : "
                + "void #inner ( #b ) { #z : : return } : return } : $ ");
        CodeGenContext ctx = new CodeGenContext(f.symbols(), "int");
        List<String> names = declarationNames(f.root(), ctx);
        assertEquals(5, new HashSet<>(names).size());
        ProgramGen.declareUserVars(f.root(), ctx);
        ProgramGen.declareUserVars(f.root(), ctx);
        assertEquals(names.stream().map(n -> "int " + n + ";\n").reduce("", String::concat),
                ctx.declarations());
    }

    @Test
    void hoistedUserDeclarationsPrecedePreviouslyAllocatedTemps() {
        Fixture f = fixture("#x : : $ ");
        CodeGenContext ctx = new CodeGenContext(f.symbols(), "float");
        String temp = ctx.newVar();
        ProgramGen.declareUserVars(f.root(), ctx);
        assertEquals("float v1;\nfloat " + temp + ";\n", ctx.declarations());
    }

    @Test
    void documentationDeclarationsUseRenamedSymbolsAndDoNotChangeContext() {
        Fixture f = fixture("#x #y : : $ ");
        CodeGenContext ctx = new CodeGenContext(f.symbols(), "float");
        ctx.newVar();
        String before = ctx.declarations();
        assertEquals("float v1;\nfloat v2;\n",
                ProgramGen.transVDecl(GenKinds.childNamed(GenKinds.childNamed(f.root(), "P"), "V_DECL"), ctx));
        assertEquals(before, ctx.declarations());
    }

    @Test
    void emptyDeclarationRulesProduceOneBlank() {
        CodeGenContext ctx = new CodeGenContext(null, "int");
        assertEquals(" ", ProgramGen.transVDecl(new Node(1, "V_DECL"), ctx));
        assertEquals(" ", ProgramGen.transFDecl(new Node(2, "F_DECL"), ctx));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plain text", "/* if */\n/* end */", "std::cout << \"*/ /*\";",
            "*\\\n/\nint escaped;", "*\\\r\n/", "*\\ \n/"})
    void codeMineIsExactlyOneSafeBlockComment(String input) {
        String comment = ProgramGen.asComment(input);
        assertTrue(comment.startsWith("/*\n"));
        assertTrue(comment.endsWith("\n*/\n"));
        String inner = comment.substring(3, comment.length() - 4);
        assertFalse(inner.contains("/*"));
        assertFalse(inner.contains("*/"));
        assertFalse(inner.contains("\\"), "Prevent preprocessor line splicing");
    }

    @Test
    void malformedProgramAndFunctionFailWithUsefulErrors() {
        CodeGenContext ctx = new CodeGenContext(null, "int");
        assertThrows(IllegalArgumentException.class, () -> ProgramGen.transSplProg(new Node(1, "P"), ctx));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> ProgramGen.transSplProg(new Node(2, "SPL_PROG"), ctx)).getMessage().contains("missing P"));
        assertThrows(IllegalArgumentException.class, () -> ProgramGen.transFType(new Node(3, "F_TYPE"), ctx));
        Node bad = new Node(4, "F_TYPE");
        bad.addChild(new Node(5, "string"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> ProgramGen.transFType(bad, ctx)).getMessage().contains("void or num"));
    }

    @Test
    void rejectsDeclarationsWithoutPhase2aSymbols() {
        SymbolTable empty = new SemanticAnalyser().analyse(new spl.tree.SyntaxTree(parse(": : $ ")));
        Node declaration = new Node(90001, "V_DECL");
        declaration.addChild(new Node(90002, "#unbound"));
        declaration.addChild(new Node(90003, "V_DECL"));
        assertThrows(IllegalStateException.class,
                () -> ProgramGen.declareUserVars(declaration, new CodeGenContext(empty, "int")));
    }

    @Test
    void completeProgramWriterUsesLfAfterStatementSemicolons() throws Exception {
        Fixture fixture = fixture(": : print ( 7 ) ; $ ");
        Path output = directory.resolve("lf.cpp");

        CodeGenMain.generate(fixture.root(), fixture.symbols(), output.toString());

        byte[] generated = Files.readAllBytes(output);
        boolean foundSemicolon = false;
        for (int i = 0; i < generated.length; i++) {
            assertFalse(generated[i] == '\r', "generated file contains a carriage return at byte " + i);
            if (generated[i] == ';') {
                foundSemicolon = true;
                assertTrue(i + 1 < generated.length && generated[i + 1] == '\n',
                        "semicolon at byte " + i + " is not followed by a line break");
            }
        }
        assertTrue(foundSemicolon);
    }

    @Nested
    @EnabledIfSystemProperty(named = "spl.codegen.integration", matches = "true")
    class Integration {
        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                ": : $ |",
                "#x : : #x = add ( 2 3 ) ; print ( #x ) ; $ |5",
                ": void #p ( ) { : : print \"hello\" ; return } : #p ( ) ; $ |hello",
                ": num #f ( #n ) { #local : : #local = mul ( #n #n ) ; return ( #local ) } : print ( #f ( 3 ) ) ; $ |9",
                ": num #f ( ) { : : return ( div ( 5 2 ) ) } : print ( #f ( ) ) ; $ |2.5",
                ": void #outer ( ) { : void #inner ( ) { : : print \"ok\" ; return } : #inner ( ) ; return } : #outer ( ) ; #outer ( ) ; $ |okok",
                ": num #f ( #n ) { : : while lesser ( #n 3 ) do { #n = add ( #n 1 ) ; } ; return ( #n ) } : print ( #f ( 1 ) ) ; print ( #f ( 2 ) ) ; $ |33",
                ": num #sq ( #a ) { : : return ( mul ( #a #a ) ) } : if eq ( #sq ( #sq ( 2 ) ) 16 ) then { print ( #sq ( 3 ) ) ; } else { nop ; } ; $ |9"
        })
        void completeProgramCompilesAndRuns(String source, String expected) throws Exception {
            Fixture f = fixture(source);
            Path output = directory.resolve("program.cpp");
            CodeGenMain.generate(f.root(), f.symbols(), output.toString());
            String generated = Files.readString(output);
            assertEquals(1, occurrences(generated, "int main()"));
            String executable = generated.substring(generated.indexOf("/* begin of executable C code */"));
            for (String name : declarationNames(f.root(), new CodeGenContext(f.symbols(), "int"))) {
                assertEquals(1, occurrences(executable, ProgramGen.detectCType(f.root()) + " " + name + ";"));
            }
            checkLabels(executable);
            assertEquals(expected == null ? "" : expected, compileAndRun(output));
        }

        @Test
        void mineIsCommentedAndDoesNotConsumeExecutableTemporaryNumbers() throws Exception {
            Fixture f = fixture(": num #unused ( #a ) { : : return ( add ( #a 1 ) ) } : print ( 2 ) ; $ ");
            String code = ProgramGen.transSplProg(f.root(), new CodeGenContext(f.symbols(), "int"));
            String executable = code.substring(code.indexOf("/* begin of executable C code */"));
            assertTrue(code.contains("The result is found in the return value f1t"));
            assertFalse(executable.contains("f1t"));
            assertTrue(executable.contains("t1 = 2;"));
            Path output = directory.resolve("mine.cpp");
            Files.writeString(output, code);
            assertEquals("2", compileAndRun(output));
        }

        @Test
        void cliGeneratesCppAtTheRequestedPath() throws Exception {
            Path input = directory.resolve("input.spl.txt"), output = directory.resolve("custom.cpp");
            Files.writeString(input, ": : print ( 7 ) ; $ ");
            Path java = Path.of(System.getProperty("java.home"), "bin", "java");
            Path classes = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            run(new ProcessBuilder(java.toString(), "-cp", classes.toString(), "spl.Main",
                    input.toString(), output.toString()).directory(directory.toFile()), "cli");
            assertTrue(Files.exists(directory.resolve("tree.xml")));
            assertEquals("7", compileAndRun(output));
        }
    }

    private String compileAndRun(Path source) throws Exception {
        Path executable = directory.resolve("program");
        run(new ProcessBuilder("g++", "-std=c++17", "-pedantic-errors", source.toString(),
                "-o", executable.toString()), "compile");
        return run(new ProcessBuilder(executable.toString()), "execute");
    }

    private String run(ProcessBuilder builder, String name) throws Exception {
        Path log = directory.resolve(name + ".log");
        Process process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail(name + " timed out");
        }
        String output = Files.readString(log);
        assertEquals(0, process.exitValue(), output);
        return output;
    }

    private static void checkLabels(String code) {
        var defined = Pattern.compile("(?m)^\\s*(L\\d+):").matcher(code);
        HashSet<String> labels = new HashSet<>();
        while (defined.find()) assertTrue(labels.add(defined.group(1)), "Duplicate label " + defined.group(1));
        var jumps = Pattern.compile("\\bgoto\\s+(L\\d+)\\s*;").matcher(code);
        while (jumps.find()) assertTrue(labels.contains(jumps.group(1)), "Undefined label " + jumps.group(1));
    }

    private static int occurrences(String text, String part) {
        int count = 0, offset = 0;
        while ((offset = text.indexOf(part, offset)) >= 0) { count++; offset += part.length(); }
        return count;
    }

    private static List<String> declarationNames(Node root, CodeGenContext ctx) {
        List<String> names = new ArrayList<>();
        if (GenKinds.is(root, "V_DECL")) {
            for (Node name : GenKinds.namesInVDecl(root)) names.add(ctx.lookUp(name));
        } else for (Node child : root.getChildren()) names.addAll(declarationNames(child, ctx));
        return names;
    }

    private static Node parse(String source) { return new Parser().parse(new Lexer().tokenize(source)).getRoot(); }

    private static Fixture fixture(String source) {
        Node root = parse(source);
        SymbolTable symbols = new spl.semantic.SemanticAnalyzer().analyse(root);
        new TypeAnalyser(symbols).analyse(root);
        return new Fixture(root, symbols);
    }
    private record Fixture(Node root, SymbolTable symbols) {}
}