package spl;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import spl.lexer.Lexer;
import spl.lexer.Token;
import spl.parser.ParseException;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyser;
import spl.semantic.SemanticException;
import spl.semantic.SymbolTable;
import spl.tree.SyntaxTree;
import spl.tree.XmlTreeWriter;
import spl.typeanalysis.TypeAnalyser;
import spl.typeanalysis.TypeException;

/**
 * Usage: java -jar spl-compiler.jar <path-to-SPL.txt>
 * On success writes tree.xml in the current directory; on a syntax or semantic
 * error, prints a meaningful message to stderr and exits non-zero.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar spl-compiler.jar <SPL.txt>");
            System.exit(1);
        }

        try {
            String source = Files.readString(new File(args[0]).toPath());

            List<Token> tokens = new Lexer().tokenize(source);
            SyntaxTree tree = new Parser().parse(tokens);

            new XmlTreeWriter().write(tree.getRoot(), new File("tree.xml"));

            SymbolTable symbols = new SemanticAnalyser().analyse(tree);
            new TypeAnalyser(symbols).analyse(tree.getRoot());

            System.out.println("OK: tree.xml written, semantic analysis passed; type analysis passed.");

        } catch (ParseException e) {
            System.err.println("Syntax Error: " + e.getMessage());
            System.exit(1);
        } catch (Lexer.LexException e) {
            System.err.println("Lexical Error: " + e.getMessage());
            System.exit(1);
        } catch (SemanticException e) {
            System.err.println("Semantic Error: " + e.getMessage());
            System.exit(1);
        } catch (TypeException e) {
            System.err.println("Type Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}