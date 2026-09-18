package spl.parser;
import spl.lexer.TokenType;
import spl.tree.Node;

public class ProgramParser {
    
    public static Node parseSplProg(ParserContext ctx){
        Node node = ctx.newNode("SPL_PROG");
        node.addChild(parseP(ctx));
        node.addChild(ctx.expectLeaf(TokenType.EOF, "at end of program"));
        return node;
    }

    public static Node parseP(ParserContext ctx){
        Node node = ctx.newNode("P");
        node.addChild(parseVDecl(ctx));
        node.addChild(ctx.expectLeaf(TokenType.COLON, "after variable declarations"));
        node.addChild(parseFDecl(ctx));
        node.addChild(ctx.expectLeaf(TokenType.COLON, "after function declarations"));
        node.addChild(parseAlgo(ctx));
        return node;
    }

    public static Node parseVDecl(ParserContext ctx){
        Node node = ctx.newNode("V_DECL");
        if(ctx.check(TokenType.USER_DEFINED_NAME)){
            node.addChild(ctx.advanceLeaf());
            node.addChild(parseVDecl(ctx));
        }

        return node;
    }

    public static Node parseFDecl(ParserContext ctx){
        Node node = ctx.newNode("F_DECL");
        if(ctx.check(TokenType.VOID) || ctx.check(TokenType.NUM_KW)){
            node.addChild(parseFType(ctx));
            node.addChild(parseFDecl(ctx));
        }

        return node;
    }

    public static Node parseFType(ParserContext ctx){
        Node node = ctx.newNode("F_TYPE");
        if(ctx.check(TokenType.VOID)){
            node.addChild(ctx.advanceLeaf());
            node.addChild(ctx.expectLeaf(TokenType.USER_DEFINED_NAME, "Before parameter list"));
            node.addChild(ctx.expectLeaf(TokenType.LPAREN, "before paramater list"));
            node.addChild(ctx.expectLeaf(TokenType.LPAREN, "before parameter list"));
            node.addChild(parseVDecl(ctx));
            node.addChild(ctx.expectLeaf(TokenType.RPAREN, "after parameter list"));
            node.addChild(ctx.expectLeaf(TokenType.LBRACE, "before function body"));
            node.addChild(parseP(ctx));
            node.addChild(ctx.expectLeaf(TokenType.RETURN, "at end of void function"));
            node.addChild(ctx.expectLeaf(TokenType.RBRACE, "after function body"));
        }else if(ctx.check(TokenType.NUM_KW)){
            node.addChild(ctx.advanceLeaf());
            node.addChild(ctx.expectLeaf(TokenType.USER_DEFINED_NAME, "as function name"));
            node.addChild(ctx.expectLeaf(TokenType.LPAREN, "before parameter list"));
            node.addChild(parseVDecl(ctx));
            node.addChild(ctx.expectLeaf(TokenType.RPAREN, "after parameter list"));
            node.addChild(ctx.expectLeaf(TokenType.LBRACE, "before function body"));
            node.addChild(parseP(ctx));
            node.addChild(ctx.expectLeaf(TokenType.RETURN, "before return value"));
            node.addChild(ctx.expectLeaf(TokenType.LPAREN, "before return expression"));
            node.addChild(ExprParser.parseTerm(ctx)) ;
            node.addChild(ctx.expectLeaf(TokenType.RPAREN, "after return expression"));
            node.addChild(ctx.expectLeaf(TokenType.RBRACE, "after function body"));
        }else{
            throw new ParseException("Expected 'void or 'num' to start a function declaration, found: '" + ctx.peek().lexeme() + "' at line" + ctx.peek().line());
        }

        return node;
    }

    public static Node parseAlgo(ParserContext ctx){
        Node node = ctx.newNode("ALGO");
        if(ctx.check(TokenType.PRINT) || ctx.check(TokenType.NOP) || ctx.check(TokenType.COMMENT)
            || ctx.check(TokenType.USER_DEFINED_NAME) || ctx.check(TokenType.IF)
            || ctx.check(TokenType.DO) || ctx.check(TokenType.WHILE) || ctx.check(TokenType.UNTIL)){
                node.addChild(InstrParser.parseInstr(ctx));
                node.addChild(ctx.expectLeaf(TokenType.SEMICOLON, "after instruction"));
                node.addChild(parseAlgo(ctx));
        }

        return node;
    }

    public static Node parseOutp(ParserContext ctx){
        Node node = ctx.newNode("OUTP");
        if(ctx.check(TokenType.LPAREN)){
            node.addChild(ctx.advanceLeaf());
            node.addChild(ExprParser.parseTerm(ctx));
            node.addChild(ctx.expectLeaf(TokenType.RPAREN, "after epxression in print statement"));
        }else if (ctx.check(TokenType.STRING)){
            node.addChild(ctx.advanceLeaf());
        }else{
            throw new ParseException("Expected '(' or a string literal, found '"
                    + ctx.peek().lexeme() + "' at line " + ctx.peek().line());
        }
        return node;
    }

    public static Node parseOutp(ParserContext ctx) {
        return null;
    }
}
