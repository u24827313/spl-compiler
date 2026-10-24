package spl;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.util.List;

import spl.codegen.CodeGenMain;
import spl.codegen.CodeGenerationException;
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
import spl.util.DiagnosticFormatter;

/** Command-line entry point for the SPL compiler. */
public class Main {

    private static final String USAGE = "java -jar spl-compiler.jar <SPL.txt> [output.cpp]";
    private static final String TREE_OUTPUT_PATH = "tree.xml";
    private static final String DEFAULT_CODE_OUTPUT_PATH = "output.cpp";

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            printArgumentError(args.length);
            System.exit(1);
            return;
        }

        String inputPath = args[0];
        String outputPath = DEFAULT_CODE_OUTPUT_PATH;
        if (args.length == 2) {
            outputPath = args[1];
        }

        String source;
        try {
            source = Files.readString(new File(inputPath).toPath());
            printInfo("Input file read: '" + inputPath + "'");
        } catch (IOException | SecurityException error) {
            String message = inputFileError(inputPath, error);
            System.err.println(DiagnosticFormatter.formatCliError("Unable to read input file", message));
            System.exit(1);
            return;
        }

        try {
            List<Token> tokens = new Lexer().tokenize(source);
            printInfo("Lexical analysis passed");

            SyntaxTree tree = new Parser().parse(tokens);
            printInfo("Syntax analysis passed");

            try {
                new XmlTreeWriter().write(tree.getRoot(), new File(TREE_OUTPUT_PATH));
                printInfo("Syntax tree written to '" + TREE_OUTPUT_PATH + "'");
            } catch (Exception error) {
                String message = outputFileError(TREE_OUTPUT_PATH, error);
                System.err.println(DiagnosticFormatter.formatCliError("Unable to write syntax tree", message));
                System.exit(1);
                return;
            }

            SymbolTable symbols = new SemanticAnalyser().analyse(tree);
            printInfo("Semantic analysis passed");

            new TypeAnalyser(symbols).analyse(tree.getRoot());
            printInfo("Type analysis passed");

            try {
                CodeGenMain.generate(tree.getRoot(), symbols, outputPath);
            } catch (IOException | InvalidPathException | SecurityException error) {
                String message = outputFileError(outputPath, error);
                System.err.println(DiagnosticFormatter.formatCliError("Unable to write output file", message));
                System.exit(1);
                return;
            }

            printInfo("Code generation passed");
            printInfo("Output file written to '" + outputPath + "'");
            System.out.println(DiagnosticFormatter.formatSuccess("Compilation completed successfully"));
        } catch (ParseException error) {
            System.err.println(DiagnosticFormatter.formatSyntaxError(error, source));
            System.exit(1);
        } catch (Lexer.LexException error) {
            System.err.println(DiagnosticFormatter.formatLexicalError(error, source));
            System.exit(1);
        } catch (SemanticException error) {
            System.err.println(DiagnosticFormatter.formatSemanticError(error));
            System.exit(1);
        } catch (TypeException error) {
            System.err.println(DiagnosticFormatter.formatTypeError(error));
            System.exit(1);
        } catch (CodeGenerationException error) {
            System.err.println(DiagnosticFormatter.formatCodeGenerationError(error));
            System.exit(1);
        } catch (Exception error) {
            System.err.println(DiagnosticFormatter.formatCliError(
                    "Unexpected compiler failure",
                    errorReason(error)
            ));
            System.exit(1);
        }
    }

    private static void printArgumentError(int argumentCount) {
        String description = "Invalid command arguments";
        String message;
        if (argumentCount == 0) {
            message = "No input file was provided.";
        } else {
            message = "Expected an input file and an optional output file, but received "
                    + argumentCount + " arguments.";
        }

        System.err.println(DiagnosticFormatter.formatUsageError(description, message, USAGE));
    }

    private static void printInfo(String message) {
        System.out.println(DiagnosticFormatter.formatInfo(message));
    }

    private static String inputFileError(String inputPath, Exception error) {
        if (error instanceof NoSuchFileException) {
            return "The input file '" + inputPath + "' does not exist.";
        }
        if (error instanceof AccessDeniedException || error instanceof SecurityException) {
            return "Permission was denied when reading '" + inputPath + "'.";
        }
        return "Could not read '" + inputPath + "': " + errorReason(error);
    }

    private static String outputFileError(String outputPath, Exception error) {
        if (error instanceof AccessDeniedException || error instanceof SecurityException) {
            return "Permission was denied when writing '" + outputPath + "'.";
        }
        return "Could not write '" + outputPath + "': " + errorReason(error);
    }

    private static String errorReason(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message;
    }
}
