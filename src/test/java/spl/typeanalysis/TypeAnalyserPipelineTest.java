package spl.typeanalysis;

import org.junit.jupiter.api.Test;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SymbolEntry;
import spl.semantic.SymbolTable;
import spl.tree.Node;
import spl.tree.SyntaxTree;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypeAnalyserPipelineTest {

    @Test
    void acceptsAnEmptyProgramAndMarksItsContainersOk() {
        AnalysedProgram program = analyse(": : $ ");

        assertEquals(Type.OK, program.context().typeOf(program.tree().getRoot()));
        assertEquals(Type.OK, program.context().typeOf(first(program.tree().getRoot(), "P")));
        assertEquals(Type.OK, program.context().typeOf(first(program.tree().getRoot(), "V_DECL")));
        assertEquals(Type.OK, program.context().typeOf(first(program.tree().getRoot(), "F_DECL")));
        assertEquals(Type.OK, program.context().typeOf(first(program.tree().getRoot(), "ALGO")));
    }

    @Test
    void typesVariableAndBothFunctionFormsInTheirSymbolEntries() {
        AnalysedProgram program = analyse("#x : void #p ( #a ) { : : return } "
                + "num #f ( #b ) { : : return ( #b ) } "
                + ": #x = #f ( 2 ) ; #p ( #x ) ; $ ");

        assertEquals(Type.NUMERIC, entry(program, "#x", SymbolEntry.Kind.VAR).getType());
        assertEquals(Type.PROCEDURE, entry(program, "#p", SymbolEntry.Kind.FUNC).getType());
        assertEquals(Type.NUMERIC, entry(program, "#f", SymbolEntry.Kind.FUNC).getType());
        assertEquals(Type.NUMERIC, entry(program, "#a", SymbolEntry.Kind.PARAM).getType());
        assertEquals(Type.NUMERIC, entry(program, "#b", SymbolEntry.Kind.PARAM).getType());
    }

    @Test
    void acceptsCallToLaterDeclarationInTheSameScope() {
        AnalysedProgram program = analyse(": void #outer ( ) { : "
                + "void #first ( ) { : : return } "
                + "void #later ( ) { : : return } "
                + ": #later ( ) ; return } : #outer ( ) ; $ ");

        assertEquals(Type.PROCEDURE, entry(program, "#later", SymbolEntry.Kind.FUNC).getType());
    }

    @Test
    void keepsTypesSeparateForSameSpellingInDifferentScopes() {
        AnalysedProgram program = analyse("#x : void #outer ( ) { : void #x ( ) { : : return } "
                + ": #x ( ) ; return } : #x = 1 ; $ ");
        SymbolEntry variable = entry(program, "#x", SymbolEntry.Kind.VAR);
        SymbolEntry function = entry(program, "#x", SymbolEntry.Kind.FUNC);

        assertNotSame(variable, function);
        assertEquals(Type.NUMERIC, variable.getType());
        assertEquals(Type.PROCEDURE, function.getType());
        assertNotEquals(variable.getUniqueName(), function.getUniqueName());
    }

    @Test
    void rejectsProcedureCallAsNumericFunctionReturnValue() {
        SyntaxTree tree = parse(": num #f ( ) { : "
            + "void #p ( ) { : : return } : return ( #p ( ) ) } : $ ");
        SymbolTable symbols = new SemanticAnalyser().analyse(tree);

        TypeException error = assertThrows(TypeException.class,
            () -> new TypeAnalyser(symbols).analyse(tree.getRoot()));

        assertEquals("Type error in rule TERM -> CALL: expected numeric but found procedure",
                error.getMessage());
    }

    private static AnalysedProgram analyse(String source) {
        SyntaxTree tree = parse(source);
        SymbolTable symbols = new SemanticAnalyser().analyse(tree);
        TypeContext context = new TypeContext(symbols);
        new TypeAnalyser(context).analyse(tree.getRoot());
        return new AnalysedProgram(tree, symbols, context);
    }

    private static SyntaxTree parse(String source) {
        return new Parser().parse(new Lexer().tokenize(source));
    }

    private static SymbolEntry entry(AnalysedProgram program, String name, SymbolEntry.Kind kind) {
        List<Node> matches = new ArrayList<>();
        collectNames(program.tree().getRoot(), name, matches);
        for (Node match : matches) {
            SymbolEntry entry = program.symbols().getEntry(match.getId());
            if (entry != null && entry.getKind() == kind && entry.getDeclNodeId() == match.getId()) {
                return entry;
            }
        }
        throw new AssertionError("No " + kind + " declaration found for " + name);
    }

    private static void collectNames(Node node, String name, List<Node> matches) {
        if (node.isLeaf() && name.equals(node.getContents())) {
            matches.add(node);
        }
        for (Node child : node.getChildren()) {
            collectNames(child, name, matches);
        }
    }

    private static Node first(Node node, String contents) {
        if (contents.equals(node.getContents())) {
            return node;
        }
        for (Node child : node.getChildren()) {
            Node found = first(child, contents);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private record AnalysedProgram(SyntaxTree tree, SymbolTable symbols, TypeContext context) {}
}