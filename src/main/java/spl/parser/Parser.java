package spl.parser;

import spl.lexer.Token;
import spl.tree.Node;

import java.util.List;

/**
 * Owner: TODO(assign teammate(s))
 *
 * NOTE: the raw SPL grammar is not LL(1) as-is — TERM and INSTR both have a
 * USER-DEFINED-NAME-prefixed conflict with CALL (see grammar/SPL-grammar.md
 * and the left-factored version noted there). Decide as a group whether to:
 *   (a) left-factor the grammar and hand-write a recursive-descent LL(1) parser, or
 *   (b) build SLR/LALR tables on the original grammar.
 * Document the decision + grammar transform in grammar/SPL-grammar.md before
 * implementing this class.
 */
public class Parser {

    private final List<Token> tokens;
    private int pos = 0;
    private int nextNodeId = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** Entry point: parses SPL_PROG -> P $ and returns the syntax tree root. */
    public Node parse() {
        throw new UnsupportedOperationException("TODO: implement parse table / recursive descent");
    }

    private int nextId() {
        return nextNodeId++;
    }
}
