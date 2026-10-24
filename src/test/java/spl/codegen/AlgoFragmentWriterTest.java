package spl.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spl.tree.Node;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlgoFragmentWriterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rendersHoistedTemporaryDeclarationsBeforeTheAlgoBody() {
        String source = ": : print ( add ( 1 2 ) ) ; $ ";
        CodeGenTestSupport.Translation translation = CodeGenTestSupport.translate(source, "int");

        String rendered = AlgoFragmentWriter.render(
                translation.algo(), translation.symbols(), "int");

        assertEquals("int t1;\nint t2;\nint t3;\n"
                        + "t2 = 1;\nt3 = 2;\nt1 = t2 + t3;\n"
                        + "std::cout << t1;\n/* end */\n",
                rendered);
        assertFalse(rendered.contains("int main"));
        assertFalse(rendered.contains("#include"));
    }

    @Test
    void usesTheExplicitNumericTypeAndRejectsUnsupportedTypeNames() {
        String source = ": : print ( 1 ) ; $ ";
        CodeGenTestSupport.Translation translation = CodeGenTestSupport.translate(source, "float");
        String rendered = AlgoFragmentWriter.render(translation.algo(), translation.symbols(), "float");

        assertTrue(rendered.startsWith("float t1;\n"));
        assertThrows(IllegalArgumentException.class,
                () -> AlgoFragmentWriter.render(translation.algo(), translation.symbols(), "long"));
    }

    @Test
    void writesUtf8WithLfLineEndingsAndNoExecutableWrapper() throws Exception {
        String source = ": : print \"hello.world\" ; print ( 3 ) ; $ ";
        CodeGenTestSupport.Translation translation = CodeGenTestSupport.translate(source, "int");
        Path output = temporaryDirectory.resolve("algo.cpp");

        AlgoFragmentWriter.write(translation.algo(), translation.symbols(), "int", output);

        byte[] bytes = Files.readAllBytes(output);
        String written = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(written.contains("std::cout << \"hello.world\";\n"));
        assertTrue(written.contains("std::cout << t1;\n"));
        assertFalse(written.contains("\r"));
        assertFalse(written.contains("int main"));
        assertFalse(written.contains("#include"));
        for (int i = 0; i < bytes.length; i++) {
            boolean isSemicolon = bytes[i] == ';';
            if (!isSemicolon) {
                continue;
            }

            boolean hasFollowingByte = i + 1 < bytes.length;
            boolean isFollowedByNewline = hasFollowingByte && bytes[i + 1] == '\n';
            assertTrue(isFollowedByNewline,
                    "semicolon at byte " + i + " is not followed by a line break");
        }
    }

    @Test
    void doesNotCreateOrTruncateOutputWhenTranslationFails() throws Exception {
        Path output = temporaryDirectory.resolve("algo.cpp");
        Files.writeString(output, "preserve me", StandardCharsets.UTF_8);
        Node malformedAlgo = new Node(400_002, "ALGO");
        malformedAlgo.addChild(new Node(400_003, "INSTR"));

        assertThrows(IllegalArgumentException.class,
                () -> AlgoFragmentWriter.write(malformedAlgo, null, "int", output));
        String existingOutput = Files.readString(output, StandardCharsets.UTF_8);
        assertEquals("preserve me", existingOutput);
    }

    @Test
    void normalisesAnyLineEndingStyleToLf() {
        assertEquals("a\nb\nc\n", AlgoFragmentWriter.toLfLineEndings("a\r\nb\rc\n"));
    }
}
