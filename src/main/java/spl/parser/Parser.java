package spl.parser;

import java.util.List;

import spl.lexer.Token;
import spl.tree.SyntaxTree;

public class Parser {
    public SyntaxTree parse(List<Token> tokens) {
        ParserContext ctx = new ParserContext(tokens);
        //Node root = ProgramParser.parseSplProg(ctx);
        //return new SyntaxTree(root);
        return null;
    }
}