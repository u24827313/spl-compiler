package spl.semantic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spl.Main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SemanticCliE2ETest {

    private record CliResult(int exitCode, String stdout, String stderr, Path treeFile) {}

    @Test
    void validProgramRunsFromTheCommandLineAndWritesATree(@TempDir Path directory) throws Exception {
        CliResult result = runMain(directory, "#x : : #x = 5 ; $ ");

        assertEquals(0, result.exitCode(), result.stderr());
        assertTrue(result.stdout().contains("semantic analysis passed"));
        assertTrue(Files.exists(result.treeFile()));
        assertTrue(Files.readString(result.treeFile()).contains("<contents>#x</contents>"));
    }

    @Test
    void functionParametersAndCallsRunThroughTheFullCli(@TempDir Path directory) throws Exception {
        CliResult result = runMain(directory,
                "#x : num #square ( #n ) { : : return ( mul ( #n #n ) ) } : #x = #square ( 2 ) ; $ ");

        assertEquals(0, result.exitCode(), result.stderr());
        assertTrue(result.stdout().contains("semantic analysis passed"));
        assertTrue(Files.exists(result.treeFile()));
    }

    @Test
    void semanticErrorPrintsTheOffendingNameAndExitsNonzero(@TempDir Path directory) throws Exception {
        CliResult result = runMain(directory, ": : #missing = 5 ; $ ");

        assertNotEquals(0, result.exitCode());
        assertTrue(result.stderr().contains("Semantic Error"), result.stderr());
        assertTrue(result.stderr().contains("#missing"), result.stderr());
        assertTrue(result.stderr().contains("node "), result.stderr());
        assertFalse(result.stdout().contains("semantic analysis passed"));
    }

    @Test
    void syntaxErrorExitsBeforeWritingATree(@TempDir Path directory) throws Exception {
        CliResult result = runMain(directory, ": : #x = ; $ ");

        assertNotEquals(0, result.exitCode());
        assertTrue(result.stderr().contains("Syntax Error"), result.stderr());
        assertFalse(Files.exists(result.treeFile()));
    }

    private static CliResult runMain(Path directory, String source) throws Exception {
        Path input = directory.resolve("input.spl.txt");
        Path stdout = directory.resolve("stdout.txt");
        Path stderr = directory.resolve("stderr.txt");
        Files.writeString(input, source);

        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Path classes = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(java.toString(), "-cp", classes.toString(),
                Main.class.getName(), input.toString())
                .directory(directory.toFile())
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile())
                .start();

        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Compiler process did not exit within 10 seconds");
        }

        return new CliResult(process.exitValue(), Files.readString(stdout), Files.readString(stderr),
                directory.resolve("tree.xml"));
    }
}
