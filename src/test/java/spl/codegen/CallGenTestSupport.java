package spl.codegen;

import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolTable;
import spl.tree.Node;
import spl.tree.SyntaxTree;
import spl.typeanalysis.TypeAnalyser;

import java.util.List;

final class CallGenTestSupport {
    private CallGenTestSupport() {}

    static AnalysedProgram analyse(String source) {
        return analyse(source, "int");
    }

    static AnalysedProgram analyse(String source, String cType) {
        List<spl.lexer.Token> tokens = new Lexer().tokenize(source);
        SyntaxTree tree = new Parser().parse(tokens);
        SymbolTable symbols = new SemanticAnalyser().analyse(tree);
        Node root = tree.getRoot();
        new TypeAnalyser(symbols).analyse(root);
        FunctionRegistry registry = FunctionRegistry.build(root, symbols);
        CodeGenContext context = new CodeGenContext(symbols, cType, registry);
        return new AnalysedProgram(root, symbols, registry, context);
    }

    static Node findFirst(Node root, String kind) {
        if (kind.equals(root.getContents())) {
            return root;
        }
        for (Node child : root.getChildren()) {
            Node match = findFirst(child, kind);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    static Node findByText(Node root, String text) {
        if (text.equals(root.getContents())) {
            return root;
        }
        for (Node child : root.getChildren()) {
            Node match = findByText(child, text);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    static Node findCallByName(Node root, String name) {
        if ("CALL".equals(root.getContents())) {
            for (Node child : root.getChildren()) {
                if (name.equals(child.getContents())) {
                    return root;
                }
            }
        }
        for (Node child : root.getChildren()) {
            Node match = findCallByName(child, name);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    static Node directChild(Node parent, String kind) {
        for (Node child : parent.getChildren()) {
            if (kind.equals(child.getContents())) {
                return child;
            }
        }
        throw new AssertionError("Missing direct child " + kind + " under " + parent.getContents());
    }

    record AnalysedProgram(Node root, SymbolTable symbols,
                           FunctionRegistry registry, CodeGenContext context) {}
}
