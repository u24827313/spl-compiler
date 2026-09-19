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
            throw new ParseException("Unexpected end of token stream while looking ahead by "
                    + offset + " token(s)");
        }

        return tokens.get(index);
    }

    public Token advance() { return tokens.get(pos++); }
    public boolean check(spl.lexer.TokenType type) { return peek().type() == type; }

    public Token expect(spl.lexer.TokenType type, String context) {
        if (!check(type)) {
            throw new ParseException("Expected " + type + " but found '"
                    + peek().lexeme() + "' " + context
                    + " at line " + peek().line() + ", column " + peek().column());
        }
        return advance();
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