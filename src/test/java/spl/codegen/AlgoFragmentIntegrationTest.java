package spl.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AlgoFragmentIntegrationTest {
    private static final int COMPILER_TIMEOUT_SECONDS = 30;
    private static final int PROGRAM_TIMEOUT_SECONDS = 10;
    private static final int COMPILER_PROBE_TIMEOUT_SECONDS = 5;
    private static final int TIMED_OUT_EXIT_CODE = -1;

    @TempDir
    Path temporaryDirectory;

    @Test
    void generatedBranchesAndNestedBooleansExecuteOnlyTheSelectedArm() throws Exception {
        assumeTrue(cppCompilerAvailable(), "g++ is not available");
        String source = "#x : : #x = 0 ; "
                + "if and ( eq ( #x 0 ) not ( larger ( #x 1 ) ) ) "
                + "then { print \"a\" ; } else { print \"b\" ; } ; "
                + "#x = 2 ; "
                + "if or ( eq ( #x 0 ) eq ( #x 2 ) ) "
                + "then { print \"c\" ; } else { print \"d\" ; } ; "
                + "if eq ( 1 2 ) then { print \"e\" ; } else { print \"f\" ; } ; $ ";

        assertProgramOutput(source, "acf", "branches");
    }

    @Test
    void generatedPrefixAndPostfixWhileAndUntilLoopsTerminateWithExpectedOutput() throws Exception {
        assumeTrue(cppCompilerAvailable(), "g++ is not available");
        String source = "#i : : "
                + "#i = 0 ; "
                + "while eq ( #i 1 ) do { #i = add ( #i 1 ) ; } ; "
                + "print ( #i ) ; "
                + "#i = 0 ; "
                + "while lesser ( #i 3 ) do { #i = add ( #i 1 ) ; } ; "
                + "print ( #i ) ; "
                + "#i = 0 ; "
                + "until eq ( #i 0 ) do { #i = add ( #i 1 ) ; } ; "
                + "print ( #i ) ; "
                + "#i = 3 ; "
                + "until eq ( #i 0 ) do { #i = sub ( #i 1 ) ; } ; "
                + "print ( #i ) ; "
                + "#i = 0 ; "
                + "do { #i = add ( #i 1 ) ; } while lesser ( #i 2 ) ; "
                + "print ( #i ) ; "
                + "#i = 2 ; "
                + "do { #i = sub ( #i 1 ) ; } until eq ( #i 0 ) ; "
                + "print ( #i ) ; $ ";

        assertProgramOutput(source, "030020", "loops");
    }

    private void assertProgramOutput(String source, String expectedOutput, String fileName) throws Exception {
        CodeGenTestSupport.Translation translation = CodeGenTestSupport.translate(source, "int");
        Path fragmentPath = temporaryDirectory.resolve(fileName + ".fragment.cpp");
        AlgoFragmentWriter.write(translation.algo(), translation.symbols(), "int", fragmentPath);

        String fragment = Files.readString(fragmentPath, StandardCharsets.UTF_8);
        String userDeclarations = CodeGenTestSupport.userVariableDeclarations(
                translation.program(), translation.symbols(), "int");
        String wrapper = "#include <iostream>\nint main() {\n"
                + userDeclarations
                + fragment
                + "return 0;\n}\n";
        Path cpp = temporaryDirectory.resolve(fileName + ".cpp");
        Files.writeString(cpp, wrapper, StandardCharsets.UTF_8);

        Path executable = temporaryDirectory.resolve(fileName);
        Result compile = run(temporaryDirectory, COMPILER_TIMEOUT_SECONDS,
                "g++", "-std=c++17", "-Wall", "-Wextra", "-o", executable.toString(), cpp.toString());
        assertEquals(0, compile.exitCode(), "g++ failed:\n" + compile.output());

        Result program = run(temporaryDirectory, PROGRAM_TIMEOUT_SECONDS, executable.toString());
        assertEquals(0, program.exitCode(), "generated program failed:\n" + program.output());
        assertEquals(expectedOutput, program.output());
    }

    private static Result run(Path workingDirectory, int timeoutSeconds, String... command) throws Exception {
        Path log = Files.createTempFile(workingDirectory, "codegen-process-", ".log");
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(log.toFile());
        Process process = processBuilder.start();

        boolean completedWithinTimeout = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completedWithinTimeout) {
            process.destroyForcibly();
            process.waitFor();
            String processOutput = Files.readString(log, StandardCharsets.UTF_8);
            String timeoutMessage = "Timed out after " + timeoutSeconds + " seconds.\n" + processOutput;
            return new Result(TIMED_OUT_EXIT_CODE, timeoutMessage);
        }

        int exitCode = process.exitValue();
        String processOutput = Files.readString(log, StandardCharsets.UTF_8);
        return new Result(exitCode, processOutput);
    }

    private static boolean cppCompilerAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("g++", "--version");
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process process = processBuilder.start();

            boolean completedWithinTimeout = process.waitFor(
                    COMPILER_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completedWithinTimeout) {
                process.destroyForcibly();
                process.waitFor();
                return false;
            }

            int exitCode = process.exitValue();
            boolean isCompilerAvailable = exitCode == 0;
            return isCompilerAvailable;
        } catch (Exception unavailableCompiler) {
            return false;
        }
    }

    private record Result(int exitCode, String output) {}
}
