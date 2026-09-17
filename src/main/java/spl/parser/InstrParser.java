package spl.parser;

import spl.lexer.Token;
import spl.lexer.TokenType;
import spl.tree.Node;

public class InstrParser {

    // INSTR -> print OUTP | nop | comment STRING | USER-DEFINED-NAME INSTR' | BRANCH | LOOP
    public static Node parseInstr(ParserContext ctx) 
    {
        Node instr = ctx.newNode("INSTR");

        if(ctx.check(TokenType.PRINT))
        {
            Token print = ctx.advance();
            instr.addChild(ctx.newNode(print.lexeme()));
            instr.addChild(ProgramParser.parseOutp(ctx));
            return instr;
        }

        if(ctx.check(TokenType.NOP)) 
        {
            Token nop = ctx.advance();
            instr.addChild(ctx.newNode(nop.lexeme()));
            return instr;
        }

        if(ctx.check(TokenType.COMMENT))
        {
            Token comment = ctx.advance();
            instr.addChild(ctx.newNode(comment.lexeme()));

            Token string = ctx.expect(
                TokenType.STRING,
                "after 'comment'"
            );
            instr.addChild(ctx.newNode(string.lexeme()));

            return instr;
        }

        if(ctx.check(TokenType.USER_DEFINED_NAME))
        {
            Token udn = ctx.advance();
            instr.addChild(parseInstrPrime(ctx, udn));
            return instr;
        }

        if(ctx.check(TokenType.IF)) 
        {
            instr.addChild(ExprParser.parseBranch(ctx));
            return instr;
        }

        if(ctx.check(TokenType.WHILE) || ctx.check(TokenType.UNTIL) || ctx.check(TokenType.DO))
        {
            instr.addChild(ExprParser.parseLoop(ctx));
            return instr;
        }

        Token found = ctx.peek();

        throw new ParseException(
            "Expected an instruction but found '" + found.lexeme() + "' at line "
            + found.line() + ", column " + found.column()
        );
    }


    // CALL -> USER-DEFINED-NAME ( INPUT )
    public static Node parseCall(ParserContext ctx) 
    {
        Token udn = ctx.expect(
            TokenType.USER_DEFINED_NAME,
            "at start of function call"
        );

        return parseCallAfterUdn(ctx, udn);
    }


    // ASSIGN -> USER-DEFINED-NAME = TERM
    public static Node parseAssign(ParserContext ctx) 
    {
        Token udn = ctx.expect(
            TokenType.USER_DEFINED_NAME,
            "at start of assignment"
        );

        return parseAssignAfterUdn(ctx, udn);
    }


    // INPUT -> ε | TERM INPUT
    public static Node parseInput(ParserContext ctx)
    {
        Node input = ctx.newNode("INPUT");

        if(ctx.check(TokenType.RPAREN))
        {
            return input;
        }

        input.addChild(ExprParser.parseTerm(ctx));
        input.addChild(parseInput(ctx));

        return input;
    }


    // INSTR' -> = TERM | ( INPUT )
    // Left-factored continuation used after parseInstr() consumes USER-DEFINED-NAME
    private static Node parseInstrPrime(ParserContext ctx, Token udn)
    {
        if(ctx.check(TokenType.EQUALS))
        {
            return parseAssignAfterUdn(ctx, udn);
        }

        if(ctx.check(TokenType.LPAREN))
        {
            return parseCallAfterUdn(ctx, udn);
        }

        Token found = ctx.peek();

        throw new ParseException(
            "Expected '=' or '(' after '" + udn.lexeme()
            + "', but found '" + found.lexeme()
            + "' at line " + found.line()
            + ", column " + found.column()
        );
    }


    // Parses the continuation of assignment after UDN has been consumed by parseInstr()
    private static Node parseAssignAfterUdn(ParserContext ctx, Token udn)
    {
        Node assign = ctx.newNode("ASSIGN");
        assign.addChild(ctx.newNode(udn.lexeme()));

        Token equals = ctx.expect(
            TokenType.EQUALS,
            "after USER-DEFINED-NAME in assignment"
        );
        assign.addChild(ctx.newNode(equals.lexeme()));

        Node term = ExprParser.parseTerm(ctx);
        assign.addChild(term);

        return assign; 
    }


    // Parses the continuation of the function-call after UDN has been consumed by parseInstr()
    private static Node parseCallAfterUdn(ParserContext ctx, Token udn)
    {
        Node call = ctx.newNode("CALL");
        call.addChild(ctx.newNode(udn.lexeme()));

        Token leftParen = ctx.expect(
            TokenType.LPAREN,
            "after function name"
        );
        call.addChild(ctx.newNode(leftParen.lexeme()));
        call.addChild(parseInput(ctx));

        Token rightParen = ctx.expect(
            TokenType.RPAREN,
            "after function call arguments"
        );
        call.addChild(ctx.newNode(rightParen.lexeme()));

        return call;
    }
}
