package spl.parser;

import spl.lexer.Token;
import spl.lexer.TokenType;
import spl.tree.Node;
import java.util.List;


public class ExprParser {

    private static final List<TokenType> BINARY_TERM_OPS = List.of(
        TokenType.MOD, TokenType.ADD, TokenType.SUB,
        TokenType.MUL, TokenType.DIV
    );

    private static final List<TokenType> COND_TOKENS = List.of(TokenType.WHILE, TokenType.UNTIL);

    private static final List<TokenType> BINARY_BOOL_OPS = List.of(TokenType.AND, TokenType.OR);
    
    private static final List<TokenType> BINARY_COMP_OPS = List.of(TokenType.EQ, TokenType.LARGER, TokenType.LESSER);


    public static Node parseTerm(ParserContext ctx) {
        if (ctx.check(TokenType.USER_DEFINED_NAME)) {
            Node term = ctx.newNode("TERM");
            Token next = ctx.peek(1);
            Node child = null;
            if (next.type() == TokenType.LPAREN) {
                child = InstrParser.parseCall(ctx) ;
            } else {
                next = ctx.advance();
                String lexeme = next.lexeme() ;
                child = ctx.newNode(lexeme);
            }
            term.addChild(child);
            return term;
        }


        if (ctx.check(TokenType.NUM)) {
            return parseNum(ctx);
        }

        if (ctx.check(TokenType.NEG)) {
            return parseNeg(ctx);
        }

        for (TokenType op : BINARY_TERM_OPS) {
            if (ctx.check(op)) {
                return parseBinaryOp(ctx);
            }
        }

        throw unexpected(ctx, "a term");
    }

    public static Node parseBranch(ParserContext ctx) {
        Node branch = ctx.newNode("BRANCH");

        addExpected(branch, ctx, TokenType.IF, "at start of branch");

        Node child = parseBool(ctx);
        branch.addChild(child);
        
        addExpected(branch, ctx, TokenType.THEN, "after branch condition");

        addExpected(branch, ctx, TokenType.LBRACE, "before the 'then' algorithm");
        
        child = ProgramParser.parseAlgo(ctx) ;
        branch.addChild(child);
        
        addExpected(branch, ctx, TokenType.RBRACE, "after the 'then' algorithm");

        addExpected(branch, ctx, TokenType.ELSE, "after the 'then' branch");
        addExpected(branch, ctx, TokenType.LBRACE, "before the 'else' algorithm");
        
        child = ProgramParser.parseAlgo(ctx) ;
        branch.addChild(child);
        
        addExpected(branch, ctx, TokenType.RBRACE, "after the 'else' algorithm");

        return branch;
    }

    public static Node parseBool(ParserContext ctx) {
        Node bool = ctx.newNode("BOOL");
        TokenType operator = ctx.peek().type();

        if (operator == TokenType.NOT) {
            Token operation = ctx.advance() ;
            String lexeme = operation.lexeme();
            Node child = ctx.newNode(lexeme) ;
            bool.addChild(child);
            addExpected(bool, ctx, TokenType.LPAREN, "after 'not'");
            child = parseBool(ctx) ;
            bool.addChild(child);
            addExpected(bool, ctx, TokenType.RPAREN, "after the operand of 'not'");
            return bool;
        }

        for (TokenType op : BINARY_BOOL_OPS) {
            if (operator == op) {
                Token operation = ctx.advance();
                String lexeme = operation.lexeme() ;
                Node child = ctx.newNode(lexeme);
                bool.addChild(child);
                addExpected(bool, ctx, TokenType.LPAREN,
                        "after boolean operator '" + operation.lexeme() + "'");

                child = parseBool(ctx);
                bool.addChild(child);
                child = parseBool(ctx);
                bool.addChild(child);
                
                addExpected(bool, ctx, TokenType.RPAREN,
                        "after the operands of '" + operation.lexeme() + "'");
                return bool;
            }
        }
        
        for (TokenType op : BINARY_COMP_OPS) {
            if (operator == op) {
                Token operation = ctx.advance();
                String lexeme = operation.lexeme();
                Node child = ctx.newNode(lexeme);
                bool.addChild(child);
                addExpected(bool, ctx, TokenType.LPAREN,
                        "after comparison operator '" + lexeme + "'");
                child = parseTerm(ctx);
                bool.addChild(child);
                child = parseTerm(ctx);
                bool.addChild(child);
                addExpected(bool, ctx, TokenType.RPAREN,
                        "after the operands of '" + lexeme + "'");
                return bool ;      
            }
        }
        

        throw unexpected(ctx, "a boolean expression");
    }

    public static Node parseLoop(ParserContext ctx) {
        Node loop = ctx.newNode("LOOP");

        for (TokenType token : COND_TOKENS) {
            if (ctx.check(token)) {
                Node child = parseCond(ctx);
                loop.addChild(child);
                child = parseBool(ctx);
                loop.addChild(child);
                addExpected(loop, ctx, TokenType.DO , "after the loop condition");
                addExpected(loop, ctx, TokenType.LBRACE, "before the loop algorithm");
                child = ProgramParser.parseAlgo(ctx);
                loop.addChild(child);
                addExpected(loop, ctx, TokenType.RBRACE, "after the loop algorithm");
                return loop;
            }
        }

        if (ctx.check(TokenType.DO)) {
            addExpected(loop, ctx, TokenType.DO, "at start of post-test loop");
            addExpected(loop, ctx, TokenType.LBRACE, "before the loop algorithm");

            Node child = ProgramParser.parseAlgo(ctx);
            loop.addChild(child);

            addExpected(loop, ctx, TokenType.RBRACE, "after the loop algorithm");

            child = parseCond(ctx);
            loop.addChild(child);
            child = parseBool(ctx);
            loop.addChild(child);

            return loop;
        }

        throw unexpected(ctx, "'while', 'until', or 'do' at start of loop");
    }


    public static Node parseCond(ParserContext ctx) {
        Node cond = ctx.newNode("COND");

        for (TokenType token : COND_TOKENS) {
            if (ctx.check(token)) {
                Token next = ctx.advance() ;
                String lexeme = next.lexeme() ;
                Node child = ctx.newNode(lexeme);
                cond.addChild(child);
                return cond;
            }
        }

        throw unexpected(ctx, "'while' or 'until'");
    }

    private static Node parseNum(ParserContext ctx) {
        Node term = ctx.newNode("TERM");
        Token number = ctx.expect(TokenType.NUM, "as numeric term");
        String lexeme = number.lexeme();
        Node child = ctx.newNode(lexeme);
        term.addChild(child);
        return term;
    }

    private static Node parseBinaryOp(ParserContext ctx) {
        Node term = ctx.newNode("TERM");

        Token operation = ctx.advance();
        String lexeme = operation.lexeme();

        Node child = ctx.newNode(lexeme);
        term.addChild(child);

        addExpected(term, ctx, TokenType.LPAREN, "after arithmetic operator '" + lexeme + "'");

        term.addChild(parseTerm(ctx));
        term.addChild(parseTerm(ctx));

        addExpected(term, ctx, TokenType.RPAREN, "after the operands of '" + lexeme + "'");

        return term;
    }

    private static Node parseNeg(ParserContext ctx) {
        Node term = ctx.newNode("TERM");

        Token neg = ctx.expect(TokenType.NEG, "at start of negated term");
        String lexeme = neg.lexeme() ;

        Node child = ctx.newNode(lexeme);
        term.addChild(child);

        addExpected(term, ctx, TokenType.LPAREN, "after 'neg'");

        child = parseTerm(ctx);
        term.addChild(child);

        addExpected(term, ctx, TokenType.RPAREN, "after the operand of 'neg'");

        return term;
    }

    private static void addExpected(
            Node parent,
            ParserContext ctx,
            TokenType type,
            String context
    ) {
        parent.addChild(ctx.newNode(ctx.expect(type, context).lexeme()));
    }

    private static ParseException unexpected(ParserContext ctx, String expected) {
        Token found = ctx.peek();
        return new ParseException(
            "Expected " + expected + " but found '" + found.lexeme()
                + "' at line " + found.line() + ", column " + found.column()
        );
    }
}
