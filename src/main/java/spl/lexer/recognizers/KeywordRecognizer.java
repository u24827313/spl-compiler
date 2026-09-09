package spl.lexer.recognizers;

import spl.lexer.Token;
import spl.lexer.TokenType;

import java.util.Map;

/**
 * Owner: TODO(assign teammate)
 *
 * Recognizes reserved keywords (void, num, return, print, nop, comment, mod,
 * add, sub, mul, div, neg, if, then, else, not, and, or, eq, larger, lesser,
 * do, while, until) and punctuation ($, :, ;, (, ), {, }, =).
 *
 * Because USER-DEFINED-NAME always starts with '#', there is never a
 * tokenization conflict with keywords — no need for maximal-munch-then-
 * keyword-table-lookup the way you would in a typical lexer.
 */
public class KeywordRecognizer implements TokenRecognizer {

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("void", TokenType.VOID),
            Map.entry("num", TokenType.NUM_KW),
            Map.entry("return", TokenType.RETURN),
            Map.entry("print", TokenType.PRINT),
            Map.entry("nop", TokenType.NOP),
            Map.entry("comment", TokenType.COMMENT),
            Map.entry("mod", TokenType.MOD),
            Map.entry("add", TokenType.ADD),
            Map.entry("sub", TokenType.SUB),
            Map.entry("mul", TokenType.MUL),
            Map.entry("div", TokenType.DIV),
            Map.entry("neg", TokenType.NEG),
            Map.entry("if", TokenType.IF),
            Map.entry("then", TokenType.THEN),
            Map.entry("else", TokenType.ELSE),
            Map.entry("not", TokenType.NOT),
            Map.entry("and", TokenType.AND),
            Map.entry("or", TokenType.OR),
            Map.entry("eq", TokenType.EQ),
            Map.entry("larger", TokenType.LARGER),
            Map.entry("lesser", TokenType.LESSER),
            Map.entry("do", TokenType.DO),
            Map.entry("while", TokenType.WHILE),
            Map.entry("until", TokenType.UNTIL)
    );

    @Override
    public Token tryMatch(String source, int pos, int line, int column) {
        // TODO: match a keyword or a punctuation symbol, requiring a trailing blank_space.
        return null;
    }
}
