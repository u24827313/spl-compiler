package spl.util;

import org.fusesource.jansi.Ansi;
import spl.lexer.Lexer;
import spl.parser.ParseException;

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

        return formatError(
                "Syntax error",
                error.getLine(),
                error.getColumn(),
                highlightedSourceLine,
                error.getMessage(),
                coloursEnabled
        );
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
}
