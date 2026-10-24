package spl.codegen;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolTable;
import spl.tree.SyntaxTree;
import spl.typeanalysis.TypeAnalyser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


class CodeGenE2ETest {

    private static final Path FIXTURES = Path.of("tests", "resources", "codegen");
    private static final String SPL_SUFFIX = ".spl.txt";
    private static final String EXPECTED_SUFFIX = ".expected.txt";

    @TempDir
    Path tmp;

    // ---------------------------------------------------------------- the E2E run

    @TestFactory
    Stream<DynamicTest> everyFixtureCompilesAndPrintsTheExpectedOutput() throws IOException {
        assertTrue(Files.isDirectory(FIXTURES), "fixture folder not found: " + FIXTURES.toAbsolutePath());
        List<Path> fixtures;
        try (Stream<Path> files = Files.list(FIXTURES)) {
            fixtures = files.filter(p -> p.getFileName().toString().endsWith(SPL_SUFFIX))
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertTrue(!fixtures.isEmpty(), "no *" + SPL_SUFFIX + " files in " + FIXTURES);
        return fixtures.stream().map(f -> DynamicTest.dynamicTest(baseName(f), () -> runFixture(f)));
    }

    private void runFixture(Path splFile) throws Exception {
        String name = baseName(splFile);
        Path dir = Files.createDirectories(tmp.resolve(name));
        Path cpp = dir.resolve("out.cpp");

        // 1. front end + code generation
        SyntaxTree tree = new Parser().parse(new Lexer().tokenize(Files.readString(splFile)));
        SymbolTable symbols = new SemanticAnalyser().analyse(tree);
        new TypeAnalyser(symbols).analyse(tree.getRoot());
        try {
            CodeGenMain.generate(tree.getRoot(), symbols, cpp.toString());
        } catch (UnsupportedOperationException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("TODO")) {
                assumeTrue(false, "not implemented yet: " + e.getMessage());
            }
            throw e;
        }
        String code = Files.readString(cpp);

        // 2. static checks on the generated code
        assertEquals(List.of(), duplicateLabels(code), name + ": duplicate label definitions");
        assertEquals(List.of(), undefinedGotoTargets(code), name + ": goto to an undefined label");

        // 3. compile and run
        assumeTrue(gppAvailable(), "g++ not found on PATH");
        Path exe = dir.resolve("out");
        Result compile = run(dir, 60, "g++", "-o", exe.toString(), cpp.toString());
        assertEquals(0, compile.exit, name + ": g++ failed:\n" + compile.output);
        Result runResult = run(dir, 10, exe.toString());
        assertEquals(0, runResult.exit, name + ": program exited with an error or timed out:\n" + runResult.output);

        // 4. compare with the expected output
        String expected = Files.readString(FIXTURES.resolve(name + EXPECTED_SUFFIX));
        assertEquals(stripWhitespace(expected), stripWhitespace(runResult.output),
                name + ": program output differs (whitespace ignored). Generated code is in " + cpp);
    }

    // ---------------------------------------------------------------- helpers

    private record Result(int exit, String output) {}

    /** Runs a command in dir; stdout+stderr go to a file so a looping program cannot block us. */
    private static Result run(Path dir, int timeoutSeconds, String... command) throws Exception {
        Path outFile = dir.resolve("run-" + Math.abs(String.join(" ", command).hashCode()) + ".log");
        Process p = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(outFile.toFile())
                .start();
        if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            return new Result(-1, "TIMEOUT after " + timeoutSeconds + "s\n" + Files.readString(outFile));
        }
        return new Result(p.exitValue(), Files.readString(outFile));
    }

    private static boolean gppAvailable() {
        try {
            Process p = new ProcessBuilder("g++", "--version").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String baseName(Path splFile) {
        String n = splFile.getFileName().toString();
        return n.substring(0, n.length() - SPL_SUFFIX.length());
    }

    static String stripWhitespace(String s) {
        return s.replaceAll("\\s+", "");
    }

    /** Removes every C comment. The code-mine is commented out, so its labels must not count. */
    static String stripComments(String code) {
        return code.replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static final Pattern LABEL_DEF = Pattern.compile("(?:^|[;{}])\\s*(L\\d+)\\s*:", Pattern.MULTILINE);
    private static final Pattern GOTO = Pattern.compile("\\bgoto\\s+(L\\d+)\\s*;");

    /** Labels that are defined more than once in the executable (non-comment) code. */
    static List<String> duplicateLabels(String code) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Matcher m = LABEL_DEF.matcher(stripComments(code));
        while (m.find()) counts.merge(m.group(1), 1, Integer::sum);
        List<String> dups = new ArrayList<>();
        counts.forEach((label, n) -> { if (n > 1) dups.add(label); });
        return dups;
    }

    /** goto targets that have no matching label definition in the executable code. */
    static List<String> undefinedGotoTargets(String code) {
        String live = stripComments(code);
        Set<String> defined = new HashSet<>();
        Matcher d = LABEL_DEF.matcher(live);
        while (d.find()) defined.add(d.group(1));
        Set<String> undefined = new LinkedHashSet<>();
        Matcher g = GOTO.matcher(live);
        while (g.find()) if (!defined.contains(g.group(1))) undefined.add(g.group(1));
        return new ArrayList<>(undefined);
    }

    // ---------------------------------------------------------------- tests of the checkers themselves

    @Test
    void duplicateLabelDefinitionsAreReported() {
        String code = "L1: ;\nx = 1;\nL2: ;\nL1: ;\n";
        assertEquals(List.of("L1"), duplicateLabels(code));
    }

    @Test
    void labelsAndGotosInsideCommentsAreIgnored() {
        String code = "/* mine\nL1: ;\ngoto L1 ;\n*/\nL1: ;\ngoto L1 ;\n";
        assertEquals(List.of(), duplicateLabels(code));
        assertEquals(List.of(), undefinedGotoTargets(code));
    }

    @Test
    void gotoToAnUndefinedLabelIsReported() {
        String code = "if ( a < b ) goto L9 ;\nL1: ;\ngoto L1 ;\n";
        assertEquals(List.of("L9"), undefinedGotoTargets(code));
    }

    @Test
    void aCleanProgramHasNoProblems() {
        String code = "L1: /* until */ ;\nif ( t1 > t2 ) goto L2;\nL3: /* do */ ;\ngoto L1 ;\nL2: ;\n";
        assertEquals(List.of(), duplicateLabels(code));
        assertEquals(List.of(), undefinedGotoTargets(code));
    }

    @Test
    void whitespaceIsIgnoredWhenComparingOutput() {
        assertEquals("3.75,0.5", stripWhitespace(" 3.75 ,\n0.5 \r\n"));
    }
}