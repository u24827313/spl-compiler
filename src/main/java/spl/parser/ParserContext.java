package spl.parser;

import java.util.List;

import spl.lexer.Token;
import spl.lexer.TokenType;
import spl.tree.Node;
import spl.util.IdGenerator;

public class ParserContext {
    private final List<Token> tokens;
    private int pos = 0;
    private final IdGenerator idGen = new IdGenerator();

    public ParserContext(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Token peek() { return tokens.get(pos); }


    public Token peek(int offset) {
        
        if (offset < 0) {
            throw new IllegalArgumentException("Lookahead offset cannot be negative: " + offset);
        }

        int index = pos + offset;

        
        if (index >= tokens.size()) {
            Token currentToken = peek();
            String message = "Unexpected end of program while looking ahead by "
                    + offset + " token(s).";
            throw new ParseException(message, currentToken);
        }

        return tokens.get(index);
    }

    public Token advance() { return tokens.get(pos++); }
    public boolean check(spl.lexer.TokenType type) { return peek().type() == type; }

    public Token expect(spl.lexer.TokenType type, String context) {
        if (!check(type)) {
            String expectedToken = describe(type);
            String foundToken = peek().lexeme();
            String message = "Expected " + expectedToken + " " + context
                    + ", but found '" + foundToken + "'.";
            throw error(message);
        }
        return advance();
    }

    public ParseException error(String message) {
        return new ParseException(message, peek());
    }

    private static String describe(TokenType type) {
        return switch (type) {
            case EOF -> "the end of the program";
            case COLON -> "':'";
            case SEMICOLON -> "';'";
            case LPAREN -> "'('";
            case RPAREN -> "')'";
            case LBRACE -> "'{'";
            case RBRACE -> "'}'";
            case EQUALS -> "'='";
            case NUM -> "a number";
            case USER_DEFINED_NAME -> "a user-defined name";
            case STRING -> "a string literal";
            default -> "'" + type.name().toLowerCase().replace("_kw", "") + "'";
        };
    }

    public Node newNode(String contents) {
        return new Node(idGen.next(), contents);
    }

    public Node leaf(Token token) {
        return newNode(token.lexeme());
    }

    public Node expectLeaf(TokenType type, String context) {
        return leaf(expect(type, context));
    }

    public Node advanceLeaf() {
        return leaf(advance());
    }
}