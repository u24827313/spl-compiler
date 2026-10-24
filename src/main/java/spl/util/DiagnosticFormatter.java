package spl.util;

import org.fusesource.jansi.Ansi;
import spl.lexer.Lexer;

/** Formats concise, source-aware diagnostics for command-line users. */
public final class DiagnosticFormatter {

    private static final boolean COLOURS_ENABLED = areColoursEnabled();

    private DiagnosticFormatter() {
    }

    public static String formatLexicalError(Lexer.LexException error, String source) {
        return formatLexicalError(error, source, COLOURS_ENABLED);
    }

    static String formatLexicalError(Lexer.LexException error, String source, boolean coloursEnabled) {
        String errorHeading = boldRed("Lexical error", coloursEnabled);
        String location = cyan("Line " + error.getLine() + ", column " + error.getColumn(), coloursEnabled);
        String relevantSourceLine = sourceLine(source, error.getLine());
        String highlightedSourceLine = highlightSourceCharacter(
                relevantSourceLine, error.getColumn(), coloursEnabled);
        String explanation = highlightQuotedText(error.getMessage(), coloursEnabled);

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

    private static String highlightSourceCharacter(String sourceLine, int column, boolean coloursEnabled) {
        int characterIndex = column - 1;
        if (characterIndex < 0) {
            return sourceLine;
        }
        if (characterIndex >= sourceLine.length()) {
            return sourceLine;
        }

        String textBeforeCharacter = sourceLine.substring(0, characterIndex);
        String highlightedCharacter = sourceLine.substring(characterIndex, characterIndex + 1);
        String textAfterCharacter = sourceLine.substring(characterIndex + 1);
        return textBeforeCharacter
                + boldRed(highlightedCharacter, coloursEnabled)
                + textAfterCharacter;
    }

    private static String highlightQuotedText(String message, boolean coloursEnabled) {
        int openingQuote = message.indexOf('\'');
        int closingQuote = message.lastIndexOf('\'');
        boolean hasOpeningQuote = openingQuote >= 0;
        boolean hasClosingQuote = closingQuote > openingQuote;
        if (!hasOpeningQuote || !hasClosingQuote) {
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
