package spl.codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import spl.semantic.SymbolTable;
import spl.tree.Node;

/** Public bridge from the analysed syntax tree to a complete C++ output file. */
public class CodeGenMain {
    /** Finish generation before opening the output file. */
    public static void generate(Node splProg, SymbolTable symT, String outPath) throws IOException {
        Objects.requireNonNull(splProg, "SPL_PROG root");
        Objects.requireNonNull(symT, "Phase 2a symbol table");
        Path output = Path.of(Objects.requireNonNull(outPath, "Output path"));
        String cType = ProgramGen.detectCType(splProg);
        FunctionRegistry registry = FunctionRegistry.build(splProg, symT);
        CodeGenContext ctx = new CodeGenContext(symT, cType, registry);
        String code = ProgramGen.transSplProg(splProg, ctx);
        Files.writeString(output, code, StandardCharsets.UTF_8);
    }
}