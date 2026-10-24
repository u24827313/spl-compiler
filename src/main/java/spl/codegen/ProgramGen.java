package spl.codegen;

import static spl.codegen.GenKinds.*;

import java.util.Objects;
import spl.tree.Node;

public class ProgramGen {

    /** SPL_PROG -> P $ ; returns trans(P) + "return 0;" + closing brace. */
    public static String transSplProg(Node splProg, CodeGenContext ctx) {
        requireKind(splProg, "SPL_PROG");
        Objects.requireNonNull(ctx, "Code generation context");
        return transP(requiredChild(splProg, P), ctx)
                + "return 0;\n}\n/* end of executable C */\n";
    }

    /** P -> V_DECL : F_DECL : ALGO */
    static String transP(Node p, CodeGenContext ctx) {
        requireKind(p, P);
        declareUserVars(p, ctx);
        CodeGenContext scratch = ctx.scratch();
        String mine = transFDecl(requiredChild(p, F_DECL), scratch);
        String mineDeclarations = scratch.declarations();
        // Generate first: only then is the complete temporary list known.
        String body = AlgoGen.transAlgo(requiredChild(p, ALGO), ctx);
        return "/* begin of the non-executable code-mine for later inlining */\n"
                + asComment(mineDeclarations + mine)
                + "/* end of the non-executable code-mine */\n"
                + "/* begin of executable C code */\n"
                + "#include <iostream>\nint main()\n{\n"
                + ctx.declarations() + body;
    }

    /** V_DECL -> eps | NAME V_DECL ; "typ name;\n" per name (empty -> one blank space). */
    static String transVDecl(Node vdecl, CodeGenContext ctx) {
        requireKind(vdecl, V_DECL);
        CodeGenContext declarations = ctx.scratch();
        for (Node name : namesInVDecl(vdecl)) declarations.declareUserVar(ctx.lookUp(name));
        String code = declarations.declarations();
        return code.isEmpty() ? " " : code;
    }

    /** F_DECL -> eps | F_TYPE F_DECL ; commented code-mine text (pass ctx.scratch()). */
    static String transFDecl(Node fDecl, CodeGenContext scratch) {
        requireKind(fDecl, F_DECL);
        StringBuilder code = new StringBuilder();
        Node current = fDecl;
        while (!current.getChildren().isEmpty()) {
            code.append(transFType(requiredChild(current, F_TYPE), scratch)).append('\n');
            current = requiredChild(current, F_DECL);
        }
        return code.length() == 0 ? " " : code.toString();
    }

    /** F_TYPE -> void/num NAME ( V_DECL ) { P return [( TERM )] } */
    static String transFType(Node fType, CodeGenContext scratch) {
        requireKind(fType, F_TYPE);
        if (fType.getChildren().isEmpty()) throw new IllegalArgumentException("F_TYPE has no header");
        String header = leafText(fType.getChildren().get(0));
        boolean isVoid = "void".equals(header);
        if (!isVoid && !"num".equals(header)) {
            throw new IllegalArgumentException("F_TYPE must begin with void or num");
        }
        Node name = firstNameChild(fType);
        if (name == null) throw new IllegalArgumentException("F_TYPE has no NAME");
        String internalName = scratch.lookUp(name);
        String description = (isVoid ? "void-procedure " : "Function ") + internalName;
        Node body = requiredChild(fType, P);
        StringBuilder code = new StringBuilder("begin of code-mine for ")
                .append(description).append('\n')
                .append("Parameters for inlining:\n")
                .append(transVDecl(requiredChild(fType, V_DECL), scratch)).append('\n')
                .append("Nested functions:\n")
                .append(transFDecl(requiredChild(body, F_DECL), scratch)).append('\n')
                .append("Local variables:\n")
                .append(transVDecl(requiredChild(body, V_DECL), scratch)).append('\n')
                .append("Body of the procedure:\n")
                .append(AlgoGen.transAlgo(requiredChild(body, ALGO), scratch));
        String result = null;
        if (!isVoid) {
            result = scratch.newVar(internalName);
            code.append(TermGen.transTerm(requiredChild(fType, TERM), result, scratch));
        }
        code.append("end of code-mine for ").append(description).append('\n');
        if (result != null) {
            code.append("The result is found in the return value ").append(result).append('\n');
        }
        return code.toString();
    }

    /**
     * Wrap generated code in a C comment. The code already contains comments
     * (/* if *&#47;, /* end *&#47; ...), so inner markers must be neutralised first,
     * e.g. replace "/*" with "/ *" and "*&#47;" with "* /", then wrap.
     */
    static String asComment(String code) {
        Objects.requireNonNull(code, "Code mine text");
        // C++ splices backslash-newline before recognising comments. Prevent a
        // splice from re-forming an otherwise separated closing delimiter.
        String safe = code.replace("\\", "[backslash]")
                .replace("/*", "/ *").replace("*/", "* /");
        return "/*\n" + safe + "\n*/\n";
    }

    /**
     * Walk EVERY V_DECL in the tree (main's, and every function's parameters and
     * locals) and call ctx.declareUserVar(ctx.lookUp(nameLeaf)) so each user
     * variable is declared exactly once at the top of main().
     */
    static void declareUserVars(Node root, CodeGenContext ctx) {
        Objects.requireNonNull(root, "Syntax tree");
        if (is(root, V_DECL)) {
            for (Node name : namesInVDecl(root)) ctx.declareUserVar(ctx.lookUp(name));
            return;
        }
        for (Node child : root.getChildren()) declareUserVars(child, ctx);
    }

    /** "float" if any NUM leaf contains a '.', otherwise "int". */
    static String detectCType(Node root) {
        Objects.requireNonNull(root, "Syntax tree");
        if (root.isLeaf()) {
            String text = root.getContents();
            if ("div".equals(text) || text.matches("-?\\d+\\.\\d+")) return "float";
        }
        for (Node child : root.getChildren()) {
            if ("float".equals(detectCType(child))) return "float";
        }
        return "int";
    }

    private static Node requiredChild(Node parent, String kind) {
        Node child = childNamed(parent, kind);
        if (child == null) {
            throw new IllegalArgumentException(parent.getContents() + " node "
                    + parent.getId() + " is missing " + kind);
        }
        return child;
    }

    private static void requireKind(Node node, String kind) {
        if (!is(node, kind)) throw new IllegalArgumentException("Expected " + kind + " node");
    }
}