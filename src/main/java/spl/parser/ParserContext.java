package spl.parser;

import spl.lexer.Token;
import spl.tree.Node;
import spl.util.IdGenerator;

import java.util.List;

public class ParserContext {
    private final List<Token> tokens;
    private int pos = 0;
    private final IdGenerator idGen = new IdGenerator();

    public ParserContext(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Token peek() { return tokens.get(pos); }
    public Token advance() { return tokens.get(pos++); }
    public boolean check(spl.lexer.TokenType type) { return peek().getType() == type; }

    public Token expect(spl.lexer.TokenType type, String context) {
        if (!check(type)) {
            throw new ParseException("Expected " + type + " but found '"
                    + peek().getLexeme() + "' " + context
                    + " at line " + peek().getLine() + ", column " + peek().getColumn());
        }
        return advance();
    }

    public Node newNode(String contents) {
        return new Node(idGen.next(), contents);
    }
}