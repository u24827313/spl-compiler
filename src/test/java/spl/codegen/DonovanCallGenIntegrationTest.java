package spl.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spl.tree.Node;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DonovanCallGenIntegrationTest {
    private static final long COMPILER_TIMEOUT_SECONDS = 45;
    private static final long PROGRAM_TIMEOUT_SECONDS = 10;

    @TempDir
    Path temporaryDirectory;

    @Test
    void executesNestedSameFunctionArgumentsInTheCorrectOrder() throws Exception {
        assertProgramOutput(": num #f ( #a #b ) { : : return ( add ( #a #b ) ) } "
                        + ": print ( #f ( 1 #f ( 2 3 ) ) ) ; $ ", "int", "6", "nested-arguments");
    }

    @Test
    void evaluatesArgumentsLeftToRightBeforeBindingParameters() throws Exception {
        String source = "#x : num #pair ( #a #b ) { : : return ( add ( mul ( #a 10 ) #b ) ) } "
                + "num #change ( ) { : : #x = 9 ; return ( 2 ) } "
                + ": #x = 4 ; print ( #pair ( #x #change ( ) ) ) ; print \",\" ; "
                + "print ( #x ) ; $ ";
        assertProgramOutput(source, "int", "42,9", "argument-side-effect");
    }

    @Test
    void executesNestedFunctionInliningAndRepeatedLoopExpansions() throws Exception {
        String source = "tests/resources/codegen/04-nested-f-calls-g.spl.txt";
        assertProgramOutput(Files.readString(Path.of(source)), "int", "7,15", "nested-functions");
    }

    @Test
    void executesSameSpelledNestedFunctionsFromDifferentScopes() throws Exception {
        String source = ": num #left ( ) { : num #helper ( ) { : : return ( 11 ) } "
                + ": return ( #helper ( ) ) } "
                + "num #right ( ) { : num #helper ( ) { : : return ( 22 ) } "
                + ": return ( #helper ( ) ) } "
                + ": print ( #left ( ) ) ; print \",\" ; print ( #right ( ) ) ; $ ";
        assertProgramOutput(source, "int", "11,22", "same-spelled-helpers");
    }

    @Test
    void executesFloatCallArgumentsAndReturns() throws Exception {
        String source = Files.readString(Path.of("tests/resources/codegen/07-float.spl.txt"));
        assertProgramOutput(source, "float", "3.75,0.5", "float-calls");
    }

    private void assertProgramOutput(String source, String cType, String expectedOutput,
                                     String fileName) throws Exception {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(source, cType);
        Node mainProgram = CallGenTestSupport.directChild(program.root(), "P");
        Node mainAlgo = CallGenTestSupport.directChild(mainProgram, "ALGO");
        declareEveryUserVariable(program.root(), program.context());
        String body = AlgoGen.transAlgo(mainAlgo, program.context());
        String declarations = program.context().declarations();
        String translationUnit = "#include <iostream>\nint main() {\n"
                + declarations + body + "return 0;\n}\n";

        assertEquals(List.of(), CodeGenE2ETest.duplicateLabels(body));
        assertEquals(List.of(), CodeGenE2ETest.undefinedGotoTargets(body));
        assertFalse(body.contains("null ="), body);

        Path cppPath = temporaryDirectory.resolve(fileName + ".cpp");
        Path executablePath = temporaryDirectory.resolve(fileName);
        Files.writeString(cppPath, translationUnit, StandardCharsets.UTF_8);

        Result compilation = run(temporaryDirectory, COMPILER_TIMEOUT_SECONDS,
                "g++", "-std=c++17", "-Wall", "-Wextra", "-o",
                executablePath.toString(), cppPath.toString());
        assertEquals(0, compilation.exitCode(), "g++ failed:\n" + compilation.output());

        Result execution = run(temporaryDirectory, PROGRAM_TIMEOUT_SECONDS, executablePath.toString());
        assertEquals(0, execution.exitCode(), "Generated executable failed:\n" + execution.output());
        assertEquals(expectedOutput, execution.output());
    }

    private static void declareEveryUserVariable(Node node, CodeGenContext context) {
        if (isKind(node, "V_DECL")) {
            List<Node> declaredNames = GenKinds.namesInVDecl(node);
            for (Node declaredName : declaredNames) {
                context.declareUserVar(context.lookUp(declaredName));
            }
        }
        for (Node child : node.getChildren()) {
            declareEveryUserVariable(child, context);
        }
    }

    private static boolean isKind(Node node, String kind) {
        return kind.equals(node.getContents());
    }

    private static Result run(Path directory, long timeoutSeconds, String... command) throws Exception {
        Path outputFile = Files.createTempFile(directory, "process-", ".log");
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(directory.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(outputFile.toFile());
        Process process = processBuilder.start();

        boolean completedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completedInTime) {
            process.destroyForcibly();
            process.waitFor();
            String output = Files.readString(outputFile, StandardCharsets.UTF_8);
            return new Result(-1, "Timed out after " + timeoutSeconds + " seconds.\n" + output);
        }

        int exitCode = process.exitValue();
        String output = Files.readString(outputFile, StandardCharsets.UTF_8);
        return new Result(exitCode, output);
    }

    private record Result(int exitCode, String output) {}
}
