package spl.lexer;

/**
 * All terminal categories of SPL: reserved keywords, punctuation,
 * and the three regex-defined lexical categories (NUM, USER-DEFINED-NAME, STRING).
 * Extend this as needed — keep it in sync with grammar/SPL-grammar.md.
 */
public enum TokenType {
    // Structural / punctuation
    EOF,            // $
    COLON,          // :
    SEMICOLON,      // ;
    LPAREN, RPAREN, // ( )
    LBRACE, RBRACE, // { }
    EQUALS,         // =

    // Keywords
    VOID, NUM_KW, RETURN, PRINT, NOP, COMMENT,
    MOD, ADD, SUB, MUL, DIV, NEG,
    IF, THEN, ELSE,
    NOT, AND, OR, EQ, LARGER, LESSER,
    DO, WHILE, UNTIL,

    // Regex-defined lexical categories (one recognizer per category, see recognizers/)
    NUM,
    USER_DEFINED_NAME,
    STRING
}
