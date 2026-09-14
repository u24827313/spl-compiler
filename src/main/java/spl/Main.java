package spl;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import spl.lexer.Lexer;
import spl.lexer.Token;
import spl.parser.ParseException;
import spl.parser.Parser;
import spl.tree.SyntaxTree;
import spl.tree.XmlTreeWriter;

/**
 * Usage: java -jar spl-compiler.jar <path-to-SPL.txt>
 * On success writes tree.xml in the current directory; on syntax error,
 * prints a meaningful message + hints to stderr and exits non-zero.
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
            SyntaxTree tree = new Parser().parse(tokens); // matches Parser.java's parse(List<Token>)

            new XmlTreeWriter().write(tree.getRoot(), new File("tree.xml"));
            System.out.println("OK: tree.xml written.");

        } catch (ParseException e) {
            System.err.println("Syntax Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}