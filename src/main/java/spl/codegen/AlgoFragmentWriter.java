package spl.codegen;

import spl.semantic.SymbolTable;
import spl.tree.Node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Writes a Phase 3a algorithm fragment, not a complete C++ translation unit. */
public final class AlgoFragmentWriter {
    private AlgoFragmentWriter() {}

    /**
     * Translates an ALGO subtree and returns hoisted temporary declarations
     * followed by the body, using LF for convenient in-process composition.
     */
    public static String render(Node algo, SymbolTable symbols, String cType) {
        requireSupportedType(cType);
        CodeGenContext context = new CodeGenContext(symbols, cType);
        String body = AlgoGen.transAlgo(algo, context);
        return context.declarations() + body;
    }

    /**
     * Writes a fragment with UTF-8 encoding and the specification's ASCII-13
     * line endings. No wrapper, include, or user-variable declarations are added.
     * Translation completes before the destination is created or truncated.
     */
    public static void write(Node algo, SymbolTable symbols, String cType, Path output) throws IOException {
        String fragment = render(algo, symbols, cType);
        String normalizedLineFeeds = fragment.replace("\r\n", "\n");
        String ascii13Lines = normalizedLineFeeds.replace('\n', '\r');
        Files.writeString(output, ascii13Lines, StandardCharsets.UTF_8);
    }

    private static void requireSupportedType(String cType) {
        boolean isIntegerType = Objects.equals(cType, "int");
        boolean isFloatingPointType = Objects.equals(cType, "float");
        boolean isSupportedType = isIntegerType || isFloatingPointType;
        if (!isSupportedType) {
            throw new IllegalArgumentException("Temporary C++ type must be 'int' or 'float'");
        }
    }
}
