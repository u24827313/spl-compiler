package spl.codegen;

import spl.semantic.SymbolTable;
import spl.tree.Node;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Wiring: declarations first (hoisted temps), then the ALGO body.
 * Person C owns this file.
 *
 * NOTE: the body is generated BEFORE declarations() is called, because the
 * temp list is only complete after translation.
 */
public class CodeGenMain {

    public static void generate(Node mainAlgo, SymbolTable symT, String cType,
                                String outPath) throws IOException {
        CodeGenContext ctx = new CodeGenContext(symT, cType);

        String body = AlgoGen.transAlgo(mainAlgo, ctx);   // must run first
        String decls = ctx.declarations();

        try (FileWriter out = new FileWriter(outPath)) {
            // TODO: add any C header/main() wrapper you need for 3b; for 3a
            // a plain dump of declarations + body is enough.
            out.write(decls);
            out.write(body);
        }
    }
}