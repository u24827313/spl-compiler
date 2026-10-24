package spl.util;

import org.junit.jupiter.api.Test;
import spl.codegen.CodeGenerationException;
import spl.lexer.Lexer;
import spl.lexer.Token;
import spl.lexer.TokenType;
import spl.parser.ParseException;
import spl.parser.SyntaxErrorType;
import spl.semantic.SemanticErrorType;
import spl.semantic.SemanticException;
import spl.tree.Node;
import spl.typeanalysis.TypeErrorType;
import spl.typeanalysis.TypeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticFormatterTest {

    private static final int MAXIMUM_ANSI_STYLE_LENGTH = 10;

    @Test
    void formatsLexicalErrorsWithTheSourceLineBeforeTheExplanation() {
        Lexer.LexException error = new Lexer.LexException("Unrecognized character '@'.", 2, 7);

        String diagnostic = DiagnosticFormatter.formatLexicalError(error, "print \"ok\"\nprint @", false);

        assertEquals("Lexical error\n"
                + "Line 2, column 7\n\n"
                + "print @\n\n"
                + "> Unrecognized character '@'.", diagnostic);
    }

    @Test
    void formatsSyntaxErrorsWithTheUnexpectedTokenAndExplanation() {
        Token unexpectedToken = new Token(TokenType.PRINT, "print", 2, 1);
        ParseException error = new ParseException(
                SyntaxErrorType.MISSING_PUNCTUATION,
                "Expected ';' after an instruction, but found 'print'.",
                unexpectedToken
        );

        String diagnostic = DiagnosticFormatter.formatSyntaxError(
                error,
                "print \"first\"\nprint \"second\"",
                false
        );

        assertEquals("Syntax error\n"
                + "Line 2, column 1\n\n"
                + "print \"second\"\n\n"
                + "> Missing punctuation : "
                                + "Expected ';' after an instruction, but found 'print'.", diagnostic);
    }

    @Test
    void formatsSemanticErrorsWithoutAnUnavailableSourceLocation() {
        SemanticException error = new SemanticException(
                SemanticErrorType.UNDECLARED_VARIABLE,
                "The variable '#missing' has not been declared in an accessible scope.",
                42
        );

        String diagnostic = DiagnosticFormatter.formatSemanticError(error, false);

        assertEquals("Semantic error\n\n"
                + "> Undeclared variable : "
                + "The variable '#missing' has not been declared in an accessible scope.\n\n"
                + "[INFO] Node ID: 42", diagnostic);
    }

    @Test
    void formatsCodeGenerationErrorsWithTheirCategoryAndNodeInformation() {
        Node callNode = new Node(42, "CALL");
        CodeGenerationException error = new CodeGenerationException(
                "The function '#show' expects 1 argument, but this call provides 0 arguments.",
                callNode
        );

        String diagnostic = DiagnosticFormatter.formatCodeGenerationError(error, false);

        assertEquals("Code generation error\n\n"
                + "> Incorrect number of function arguments : "
                + "The function '#show' expects 1 argument, but this call provides 0 arguments.\n\n"
                + "[INFO] Node ID: 42", diagnostic);
    }

    @Test
    void formatsTypeErrorsWithTheirCategoryAndNodeInformation() {
        Node offendingNode = new Node(42, "CALL");
        TypeException error = new TypeException(
                TypeErrorType.INCOMPATIBLE_TYPES,
                "Expected a procedure, but found a numeric value.",
                offendingNode
        );

        String diagnostic = DiagnosticFormatter.formatTypeError(error, false);

        assertEquals("Type error\n\n"
                + "> Incompatible types : Expected a procedure, but found a numeric value.\n\n"
                + "[INFO] Node ID: 42", diagnostic);
    }

    @Test
    void highlightsTheActualTypeFoundInAnIncompatibleTypesError() {
        TypeException error = new TypeException(
                TypeErrorType.INCOMPATIBLE_TYPES,
                "Expected a procedure, but found a numeric value.",
                new Node(42, "CALL")
        );

        String diagnostic = DiagnosticFormatter.formatTypeError(error, true);

        assertTrue(diagnostic.contains("but found a \u001B["));
        assertTrue(diagnostic.contains("numeric value\u001B["));
    }

    @Test
    void highlightsBothOperatorsInAnIncompatibleNumericOperationsError() {
        TypeException error = new TypeException(
                TypeErrorType.INCOMPATIBLE_NUMERIC_OPERATIONS,
                "This program uses both 'mod' and 'div', but they are incompatible.",
                new Node(42, "SPL_PROG")
        );

        String diagnostic = DiagnosticFormatter.formatTypeError(error, true);

        assertQuotedTextIsStyled(diagnostic, "'mod'");
        assertQuotedTextIsStyled(diagnostic, "'div'");
    }

    @Test
    void coloursOnlyTheInfoLabelInSemanticNodeMetadata() {
        SemanticException error = new SemanticException(
                SemanticErrorType.UNDECLARED_VARIABLE,
                "The variable '#missing' has not been declared.",
                42
        );

        String diagnostic = DiagnosticFormatter.formatSemanticError(error, true);

        assertTrue(diagnostic.contains("[\u001B["));
        assertTrue(diagnostic.contains("INFO\u001B["));
        assertTrue(diagnostic.contains("] Node ID: 42"));
    }

    private static void assertQuotedTextIsStyled(String diagnostic, String quotedText) {
        int quotedTextIndex = diagnostic.indexOf(quotedText);
        assertTrue(quotedTextIndex >= 0);

        int styleStart = Math.max(0, quotedTextIndex - MAXIMUM_ANSI_STYLE_LENGTH);
        String textBeforeQuotedText = diagnostic.substring(styleStart, quotedTextIndex);
        assertTrue(textBeforeQuotedText.contains("\u001B["));
    }

    @Test
    void highlightsTheOffendingCharacterAndItsDescriptionWhenColoursAreEnabled() {
        Lexer.LexException error = new Lexer.LexException("Unrecognized character '@'.", 1, 7);

        String diagnostic = DiagnosticFormatter.formatLexicalError(error, "print @", true);

        assertTrue(diagnostic.contains("print \u001B["));

        int quotedCharacterIndex = diagnostic.indexOf("'@'");
        assertTrue(quotedCharacterIndex >= 0);

        int styleStart = Math.max(0, quotedCharacterIndex - MAXIMUM_ANSI_STYLE_LENGTH);
        String textBeforeQuotedCharacter = diagnostic.substring(styleStart, quotedCharacterIndex);
        assertTrue(textBeforeQuotedCharacter.contains("\u001B["));
    }
}
