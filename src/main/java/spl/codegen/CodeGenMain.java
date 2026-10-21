package spl.codegen;

import spl.semantic.SymbolTable;
import spl.tree.Node;

import java.io.IOException;


public class CodeGenMain {

    public static void generate(Node splProg, SymbolTable symT, String outPath) throws IOException {
        // TODO (Person 3):
        //   cType    = ProgramGen.detectCType(splProg)
        //   registry = FunctionRegistry.build(splProg, symT)
        //   ctx      = new CodeGenContext(symT, cType, registry)
        //   ProgramGen.declareUserVars(splProg, ctx)
        //   code     = ProgramGen.transSplProg(splProg, ctx)   // generates the body first
        //   write code to outPath (declarations are part of what transSplProg returns)
        throw new UnsupportedOperationException("TODO (3b, Person 3): CodeGenMain.generate");
    }
}