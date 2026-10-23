package spl.codegen;

import spl.lexer.Lexer;
import spl.lexer.Token;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolEntry;
import spl.semantic.SymbolTable;
import spl.tree.Node;
import spl.tree.SyntaxTree;
import spl.typeanalysis.TypeAnalyser;

import java.util.List;

final class CodeGenTestSupport {
    private CodeGenTestSupport() {}

    static Translation translate(String source, String cType) {
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(source);
        Parser parser = new Parser();
        SyntaxTree tree = parser.parse(tokens);
        SemanticAnalyser semanticAnalyser = new SemanticAnalyser();
        SymbolTable symbols = semanticAnalyser.analyse(tree);
        TypeAnalyser typeAnalyser = new TypeAnalyser(symbols);

        Node root = tree.getRoot();
        typeAnalyser.analyse(root);
        Node program = directChild(root, "P");
        Node algo = directChild(program, "ALGO");
        CodeGenContext context = new CodeGenContext(symbols, cType);
        String body = AlgoGen.transAlgo(algo, context);
        return new Translation(program, algo, symbols, context, body);
    }

    static Node directChild(Node node, String kind) {
        for (Node child : node.getChildren()) {
            if (kind.equals(child.getContents())) {
                return child;
            }
        }
        throw new AssertionError("Missing direct child " + kind + " below " + node.getContents());
    }

    static String userVariableDeclarations(Node program, SymbolTable symbols, String cType) {
        Node declarations = directChild(program, "V_DECL");
        StringBuilder out = new StringBuilder();
        for (Node name : GenKinds.namesInVDecl(declarations)) {
            SymbolEntry entry = symbols.getEntry(name.getId());
            if (entry == null) {
                throw new AssertionError("Missing symbol-table entry for declaration node " + name.getId());
            }
            out.append(cType).append(' ').append(entry.getUniqueName()).append(";\n");
        }
        return out.toString();
    }

    record Translation(Node program, Node algo, SymbolTable symbols,
                       CodeGenContext context, String body) {}
}
