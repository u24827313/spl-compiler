package spl.util;

import org.fusesource.jansi.Ansi;
import spl.codegen.CodeGenerationException;
import spl.lexer.Lexer;
import spl.parser.ParseException;
import spl.semantic.SemanticException;
import spl.typeanalysis.TypeErrorType;
import spl.typeanalysis.TypeException;

/** Formats concise, source-aware diagnostics for command-line users. */
public final class DiagnosticFormatter {

    private static final boolean COLOURS_ENABLED = areColoursEnabled();

    private DiagnosticFormatter() {
    }

    public static String formatLexicalError(Lexer.LexException error, String source) {
        return formatLexicalError(error, source, COLOURS_ENABLED);
    }

    static String formatLexicalError(Lexer.LexException error, String source, boolean coloursEnabled) {
        String relevantSourceLine = sourceLine(source, error.getLine());
        String highlightedSourceLine = highlightSourceRange(
                relevantSourceLine, error.getColumn(), 1, coloursEnabled);

        return formatError(
                "Lexical error",
                error.getLine(),
                error.getColumn(),
                highlightedSourceLine,
                error.getMessage(),
                coloursEnabled
        );
    }

    public static String formatSyntaxError(ParseException error, String source) {
        return formatSyntaxError(error, source, COLOURS_ENABLED);
    }

    static String formatSyntaxError(ParseException error, String source, boolean coloursEnabled) {
        String relevantSourceLine = sourceLine(source, error.getLine());
        int tokenLength = error.getOffendingText().length();
        String highlightedSourceLine = highlightSourceRange(
                relevantSourceLine, error.getColumn(), tokenLength, coloursEnabled);

        String explanation = error.getDescription() + " : " + error.getMessage();
        return formatError(
                "Syntax error",
                error.getLine(),
                error.getColumn(),
                highlightedSourceLine,
                explanation,
                coloursEnabled
        );
    }

    public static String formatSemanticError(SemanticException error) {
        return formatSemanticError(error, COLOURS_ENABLED);
    }

    static String formatSemanticError(SemanticException error, boolean coloursEnabled) {
        String errorHeading = boldRed("Semantic error", coloursEnabled);
        String message = error.getDescription() + " : " + error.getMessage();
        String explanation = highlightLastQuotedText(message, coloursEnabled);
        String nodeInformation = formatNodeInformation(error.getNodeId(), coloursEnabled);

        return errorHeading
                + "\n\n> "
                + explanation
                + "\n\n"
                + nodeInformation;
    }

    public static String formatCodeGenerationError(CodeGenerationException error) {
        return formatCodeGenerationError(error, COLOURS_ENABLED);
    }

    static String formatCodeGenerationError(
            CodeGenerationException error,
            boolean coloursEnabled
    ) {
        String errorHeading = boldRed("Code generation error", coloursEnabled);
        String message = error.getDescription() + " : " + error.getMessage();
        String explanation = highlightLastQuotedText(message, coloursEnabled);
        String nodeInformation = formatNodeInformation(error.getNode().getId(), coloursEnabled);

        return errorHeading
                + "\n\n> "
                + explanation
                + "\n\n"
                + nodeInformation;
    }

    public static String formatTypeError(TypeException error) {
        return formatTypeError(error, COLOURS_ENABLED);
    }

    static String formatTypeError(TypeException error, boolean coloursEnabled) {
        String errorHeading = boldRed("Type error", coloursEnabled);
        String message = error.getDescription() + " : " + error.getMessage();
        String explanation = highlightTypeExplanation(error.getErrorType(), message, coloursEnabled);

        String diagnostic = errorHeading
                + "\n\n> "
                + explanation;
        if (error.getNode() == null) {
            return diagnostic;
        }

        String nodeInformation = formatNodeInformation(error.getNode().getId(), coloursEnabled);
        return diagnostic
                + "\n\n"
                + nodeInformation;
    }

    private static String formatNodeInformation(int nodeId, boolean coloursEnabled) {
        String informationLabel = magenta("INFO", coloursEnabled);
        return "[" + informationLabel + "] Node ID: " + nodeId;
    }

    private static String formatError(
            String errorTitle,
            int line,
            int column,
            String highlightedSourceLine,
            String message,
            boolean coloursEnabled
    ) {
        String errorHeading = boldRed(errorTitle, coloursEnabled);
        String location = cyan("Line " + line + ", column " + column, coloursEnabled);
        String explanation = highlightLastQuotedText(message, coloursEnabled);

        return errorHeading
                + "\n"
                + location
                + "\n\n"
                + highlightedSourceLine
                + "\n\n> "
                + explanation;
    }

    private static boolean areColoursEnabled() {
        boolean hasInteractiveConsole = System.console() != null;
        if (!hasInteractiveConsole) {
            return false;
        }

        String noColourPreference = System.getenv("NO_COLOR");
        return noColourPreference == null;
    }

    private static String sourceLine(String source, int lineNumber) {
        String[] lines = source.split("\\R", -1);
        if (lineNumber < 1 || lineNumber > lines.length) {
            return "";
        }
        return lines[lineNumber - 1];
    }

    private static String highlightSourceRange(
            String sourceLine,
            int column,
            int length,
            boolean coloursEnabled
    ) {
        int startIndex = column - 1;
        if (startIndex < 0) {
            return sourceLine;
        }
        if (startIndex >= sourceLine.length()) {
            return sourceLine;
        }

        int endIndex = Math.min(startIndex + length, sourceLine.length());
        String textBeforeRange = sourceLine.substring(0, startIndex);
        String highlightedText = sourceLine.substring(startIndex, endIndex);
        String textAfterRange = sourceLine.substring(endIndex);
        return textBeforeRange
                + boldRed(highlightedText, coloursEnabled)
                + textAfterRange;
    }

    private static String highlightTypeExplanation(
            TypeErrorType errorType,
            String message,
            boolean coloursEnabled
    ) {
        if (errorType == TypeErrorType.INCOMPATIBLE_TYPES) {
            return highlightFoundType(message, coloursEnabled);
        }
        if (errorType == TypeErrorType.INCOMPATIBLE_NUMERIC_OPERATIONS) {
            return highlightAllQuotedText(message, coloursEnabled);
        }
        return highlightLastQuotedText(message, coloursEnabled);
    }

    private static String highlightFoundType(String message, boolean coloursEnabled) {
        String foundMarker = "but found ";
        int markerIndex = message.lastIndexOf(foundMarker);
        if (markerIndex < 0) {
            return message;
        }

        int typeStart = markerIndex + foundMarker.length();
        if (message.startsWith("an ", typeStart)) {
            typeStart += 3;
        } else if (message.startsWith("a ", typeStart)) {
            typeStart += 2;
        }

        int typeEnd = message.indexOf('.', typeStart);
        if (typeEnd < 0) {
            typeEnd = message.length();
        }

        String textBeforeType = message.substring(0, typeStart);
        String foundType = message.substring(typeStart, typeEnd);
        String textAfterType = message.substring(typeEnd);
        return textBeforeType
                + boldRed(foundType, coloursEnabled)
                + textAfterType;
    }

    private static String highlightAllQuotedText(String message, boolean coloursEnabled) {
        StringBuilder highlightedMessage = new StringBuilder();
        int searchStart = 0;

        while (searchStart < message.length()) {
            int openingQuote = message.indexOf('\'', searchStart);
            if (openingQuote < 0) {
                highlightedMessage.append(message.substring(searchStart));
                break;
            }

            int closingQuote = message.indexOf('\'', openingQuote + 1);
            if (closingQuote < 0) {
                highlightedMessage.append(message.substring(searchStart));
                break;
            }

            highlightedMessage.append(message, searchStart, openingQuote);
            String quotedText = message.substring(openingQuote, closingQuote + 1);
            highlightedMessage.append(boldRed(quotedText, coloursEnabled));
            searchStart = closingQuote + 1;
        }

        return highlightedMessage.toString();
    }

    private static String highlightLastQuotedText(String message, boolean coloursEnabled) {
        int closingQuote = message.lastIndexOf('\'');
        if (closingQuote < 0) {
            return message;
        }

        int openingQuote = message.lastIndexOf('\'', closingQuote - 1);
        if (openingQuote < 0) {
            return message;
        }

        String textBeforeQuote = message.substring(0, openingQuote);
        String quotedText = message.substring(openingQuote, closingQuote + 1);
        String textAfterQuote = message.substring(closingQuote + 1);
        return textBeforeQuote
                + boldRed(quotedText, coloursEnabled)
                + textAfterQuote;
    }

    private static String boldRed(String text, boolean coloursEnabled) {
        if (!coloursEnabled) {
            return text;
        }

        return Ansi.ansi().bold().fg(Ansi.Color.RED).a(text).reset().toString();
    }

    private static String cyan(String text, boolean coloursEnabled) {
        if (!coloursEnabled) {
            return text;
        }

        return Ansi.ansi().fg(Ansi.Color.CYAN).a(text).reset().toString();
    }

    private static String magenta(String text, boolean coloursEnabled) {
        if (!coloursEnabled) {
            return text;
        }

        return Ansi.ansi().fg(Ansi.Color.MAGENTA).a(text).reset().toString();
    }
}
