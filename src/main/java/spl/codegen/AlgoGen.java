package spl.codegen;

import java.util.List;
import spl.tree.Node;

import static spl.codegen.GenKinds.*;

/**
 * Person C: translation of ALGO, INSTR, ASSIGN, BRANCH and LOOP.
 *
 * transAlgo works on ANY ALGO subtree (not just the main program), because
 * 3b will reuse it for inlined function bodies.
 */
public class AlgoGen {

    /** ALGO -> eps | INSTR ; ALGO */
    public static String transAlgo(Node algo, CodeGenContext ctx) {
        requireKind(algo, ALGO);
        List<Node> algoChildren = algo.getChildren();
        if (algoChildren.isEmpty()) {
            return "/* end */\n";
        }
        if (algoChildren.size() != 3) {
            throw malformed(algo, "expected INSTR, ';', ALGO or epsilon");
        }

        Node instructionNode = algoChildren.get(0);
        Node separatorNode = algoChildren.get(1);
        Node remainingAlgoNode = algoChildren.get(2);
        boolean startsWithInstruction = is(instructionNode, INSTR);
        boolean hasSemicolonSeparator = is(separatorNode, ";");
        boolean endsWithAlgo = is(remainingAlgoNode, ALGO);
        boolean hasValidRecursiveShape = startsWithInstruction
                && hasSemicolonSeparator
                && endsWithAlgo;
        if (!hasValidRecursiveShape) {
            throw malformed(algo, "expected INSTR, ';', ALGO or epsilon");
        }

        String instructionCode = transInstr(instructionNode, ctx);
        String remainingAlgoCode = transAlgo(remainingAlgoNode, ctx);
        return instructionCode + remainingAlgoCode;
    }

    /** INSTR -> print | nop | comment | ASSIGN | BRANCH | LOOP | CALL(3b) */
    public static String transInstr(Node instr, CodeGenContext ctx) {
        requireKind(instr, INSTR);
        List<Node> children = instr.getChildren();
        if (children.isEmpty()) {
            throw malformed(instr, "instruction has no alternative");
        }

        Node first = children.get(0);
        String alternative = first.getContents();
        switch (alternative) {
            case "nop":
                requireLeafToken(first, "nop", instr);
                requireChildCount(instr, 1, "nop instruction takes no operands");
                return "/* nop */\n";
            case "comment":
                requireLeafToken(first, "comment", instr);
                requireChildCount(instr, 2, "comment instruction requires one STRING operand");
                Node stringNode = children.get(1);
                String commentLexeme = quotedString(stringNode, instr);
                boolean containsCommentTerminator = commentLexeme.contains("*/");
                boolean containsLineFeed = commentLexeme.contains("\n");
                boolean containsCarriageReturn = commentLexeme.contains("\r");
                boolean containsUnsafeCommentText = containsCommentTerminator
                        || containsLineFeed
                        || containsCarriageReturn;
                if (containsUnsafeCommentText) {
                    throw malformed(instr, "comment text cannot contain a comment terminator or line break");
                }

                String commentText = stripQuotes(commentLexeme);
                return "/* " + commentText + " */\n";
            case "print":
                requireLeafToken(first, "print", instr);
                requireChildCount(instr, 2, "print instruction requires one OUTP child");
                if (!is(children.get(1), OUTP)) {
                    throw malformed(instr, "expected OUTP after print");
                }
                return transPrint(children.get(1), instr, ctx);
            case ASSIGN:
                requireChildCount(instr, 1, "ASSIGN must be the only instruction child");
                return transAssign(first, ctx);
            case BRANCH:
                requireChildCount(instr, 1, "BRANCH must be the only instruction child");
                return transBranch(first, ctx);
            case LOOP:
                requireChildCount(instr, 1, "LOOP must be the only instruction child");
                return transLoop(first, ctx);
            case CALL:
                requireChildCount(instr, 1, "CALL must be the only instruction child");
                throw new UnsupportedOperationException("3b: function call as instruction");
            default:
                throw new IllegalArgumentException("Unknown INSTR alternative '" + alternative
                        + "' at node " + instr.getId());
        }
    }

    private static void requireKind(Node node, String expected) {
        if (node == null) {
            throw new IllegalArgumentException("Expected " + expected + " node but found null");
        }

        boolean hasExpectedKind = is(node, expected);
        if (!hasExpectedKind) {
            String foundKind = "'" + node.getContents() + "'";
            String nodeId = " at node " + node.getId();
            throw new IllegalArgumentException("Expected " + expected + " node but found "
                    + foundKind + nodeId);
        }
    }

    private static void requireChildCount(Node node, int expected, String detail) {
        if (node.getChildren().size() != expected) {
            throw malformed(node, detail + "; found " + node.getChildren().size() + " children");
        }
    }

    private static void requireLeafToken(Node token, String expected, Node parent) {
        if (!token.isLeaf()) {
            throw malformed(parent, "expected '" + expected + "' token leaf");
        }

        String actualToken = token.getContents();
        if (!expected.equals(actualToken)) {
            throw malformed(parent, "expected '" + expected + "' token leaf");
        }
    }

    private static String quotedString(Node string, Node parent) {
        if (!string.isLeaf()) {
            throw malformed(parent, "expected a quoted STRING operand");
        }

        String text = string.getContents();
        if (text.length() < 2) {
            throw malformed(parent, "expected a quoted STRING operand");
        }

        boolean hasOpeningQuote = text.startsWith("\"");
        boolean hasClosingQuote = text.endsWith("\"");
        boolean hasQuoteDelimiters = hasOpeningQuote && hasClosingQuote;
        if (!hasQuoteDelimiters) {
            throw malformed(parent, "expected a quoted STRING operand");
        }
        return text;
    }

    private static IllegalArgumentException malformed(Node node, String detail) {
        return new IllegalArgumentException("Malformed " + node.getContents() + " node "
                + node.getId() + ": " + detail);
    }

    /** OUTP -> STRING | '(' TERM ')' */
    private static String transPrint(Node outp, Node instr, CodeGenContext ctx) {
        requireKind(outp, OUTP);
        List<Node> outpChildren = outp.getChildren();

        if (outpChildren.size() == 1) {
            Node stringNode = outpChildren.get(0);
            String stringLiteral = quotedString(stringNode, instr);
            return "std::cout << " + stringLiteral + ";\n";
        }
        if (outpChildren.size() != 3) {
            throw malformed(instr, "OUTP must contain a quoted STRING or '(' TERM ')'");
        }

        Node openingParenthesis = outpChildren.get(0);
        Node term = outpChildren.get(1);
        Node closingParenthesis = outpChildren.get(2);
        boolean hasOpeningParenthesis = is(openingParenthesis, "(");
        boolean hasTerm = is(term, TERM);
        boolean hasClosingParenthesis = is(closingParenthesis, ")");
        boolean hasParenthesizedTerm = hasOpeningParenthesis && hasTerm && hasClosingParenthesis;
        if (!hasParenthesizedTerm) {
            throw malformed(instr, "OUTP must contain a quoted STRING or '(' TERM ')'");
        }

        String resultVariable = ctx.newVar();
        String termCode = TermGen.transTerm(term, resultVariable, ctx);
        String printStatement = "std::cout << " + resultVariable + ";\n";
        return termCode + printStatement;
    }

    /** ASSIGN -> NAME '=' TERM */
    private static String transAssign(Node assign, CodeGenContext ctx) {
        requireKind(assign, ASSIGN);
        requireChildCount(assign, 3, "ASSIGN requires NAME, '=', and TERM");
        List<Node> children = assign.getChildren();
        Node name = children.get(0);
        if (!isUserDefinedName(name)) {
            throw malformed(assign, "expected a user-defined NAME leaf on the left side");
        }
        requireLeafToken(children.get(1), "=", assign);
        Node term = children.get(2);
        requireKind(term, TERM);

        String targetName = ctx.lookUp(name);
        String resultVariable = ctx.newVar();
        String termCode = TermGen.transTerm(term, resultVariable, ctx);
        String assignmentStatement = targetName + " = " + resultVariable + ";\n";
        return termCode + assignmentStatement;
    }

    /**
     * BRANCH -> if BOOL then { ALGO_true } else { ALGO_false }
     *
     * Layout:   (comment "if")  decisionCode(BOOL, La, Lb)
     *           Lb: (comment "else")  falseCode  goto Lc;
     *           La: (comment "then")  trueCode
     *           Lc: (comment "continue")
     */
    private static String transBranch(Node branch, CodeGenContext ctx) {
        requireKind(branch, BRANCH);
        requireChildCount(branch, 10,
                "BRANCH requires if, BOOL, then, '{', ALGO, '}', else, '{', ALGO, '}'");
        List<Node> branchChildren = branch.getChildren();
        Node ifKeyword = branchChildren.get(0);
        Node conditionNode = branchChildren.get(1);
        Node thenKeyword = branchChildren.get(2);
        Node thenOpeningBrace = branchChildren.get(3);
        Node trueAlgo = branchChildren.get(4);
        Node thenClosingBrace = branchChildren.get(5);
        Node elseKeyword = branchChildren.get(6);
        Node elseOpeningBrace = branchChildren.get(7);
        Node falseAlgo = branchChildren.get(8);
        Node elseClosingBrace = branchChildren.get(9);

        requireLeafToken(ifKeyword, "if", branch);
        requireKind(conditionNode, BOOL);
        requireLeafToken(thenKeyword, "then", branch);
        requireLeafToken(thenOpeningBrace, "{", branch);
        requireKind(trueAlgo, ALGO);
        requireLeafToken(thenClosingBrace, "}", branch);
        requireLeafToken(elseKeyword, "else", branch);
        requireLeafToken(elseOpeningBrace, "{", branch);
        requireKind(falseAlgo, ALGO);
        requireLeafToken(elseClosingBrace, "}", branch);

        String trueLabel = ctx.newLabel();
        String falseLabel = ctx.newLabel();
        String endLabel = ctx.newLabel();
        String conditionCode = BoolGen.transBool(conditionNode, trueLabel, falseLabel, ctx);
        String falseBranchCode = transAlgo(falseAlgo, ctx);
        String trueBranchCode = transAlgo(trueAlgo, ctx);

        StringBuilder branchCode = new StringBuilder();
        branchCode.append(conditionCode);
        branchCode.append(label(falseLabel, "else"));
        branchCode.append(falseBranchCode);
        branchCode.append("goto ").append(endLabel).append(";\n");
        branchCode.append(label(trueLabel, "then"));
        branchCode.append(trueBranchCode);
        branchCode.append(label(endLabel, ""));
        return branchCode.toString();
    }

    /**
     * LOOP: until, while, do-until, do-while.
     *  until BOOL do { ALGO }    : ls: decision(BOOL, le, d) d: body goto ls; le:
     *  while BOOL do { ALGO }    : until not(BOOL) do { ALGO }
     *  do { ALGO } while BOOL    : ld: body decision(BOOL, ld, le) le:
     *  do { ALGO } until BOOL    : do { ALGO } while not(BOOL)
     *
     * For the negated cases, use BoolGen's semantic negation helper; simply
     * swapping target labels does not negate under the true-jump/fall-through contract.
     */
    private static String transLoop(Node loop, CodeGenContext ctx) {
        requireKind(loop, LOOP);
        List<Node> children = loop.getChildren();
        if (children.isEmpty()) {
            throw malformed(loop, "LOOP has no production children");
        }
        if (is(children.get(0), COND)) {
            return transPrefixLoop(loop, ctx);
        }
        if (is(children.get(0), "do")) {
            return transPostfixLoop(loop, ctx);
        }
        throw malformed(loop, "expected prefix COND or postfix 'do' loop layout");
    }

    /** LOOP -> COND BOOL do '{' ALGO '}' */
    private static String transPrefixLoop(Node loop, CodeGenContext ctx) {
        requireChildCount(loop, 6, "prefix LOOP requires COND, BOOL, do, '{', ALGO, '}'");
        List<Node> loopChildren = loop.getChildren();
        Node condNode = loopChildren.get(0);
        Node conditionNode = loopChildren.get(1);
        Node doKeyword = loopChildren.get(2);
        Node openingBrace = loopChildren.get(3);
        Node bodyAlgo = loopChildren.get(4);
        Node closingBrace = loopChildren.get(5);

        String conditionKind = conditionKind(condNode, loop);
        requireKind(conditionNode, BOOL);
        requireLeafToken(doKeyword, "do", loop);
        requireLeafToken(openingBrace, "{", loop);
        requireKind(bodyAlgo, ALGO);
        requireLeafToken(closingBrace, "}", loop);

        String startLabel = ctx.newLabel();
        String exitLabel = ctx.newLabel();
        String bodyLabel = ctx.newLabel();
        boolean isWhileCondition = conditionKind.equals("while");
        String conditionCode;
        if (isWhileCondition) {
            conditionCode = BoolGen.transNot(conditionNode, exitLabel, bodyLabel, ctx);
        } else {
            conditionCode = BoolGen.transBool(conditionNode, exitLabel, bodyLabel, ctx);
        }
        String bodyCode = transAlgo(bodyAlgo, ctx);

        StringBuilder loopCode = new StringBuilder();
        loopCode.append(label(startLabel, "loop"));
        loopCode.append(conditionCode);
        loopCode.append(label(bodyLabel, "do"));
        loopCode.append(bodyCode);
        loopCode.append("goto ").append(startLabel).append(";\n");
        loopCode.append(label(exitLabel, "exit"));
        return loopCode.toString();
    }

    /** LOOP -> do '{' ALGO '}' COND BOOL */
    private static String transPostfixLoop(Node loop, CodeGenContext ctx) {
        requireChildCount(loop, 6, "postfix LOOP requires do, '{', ALGO, '}', COND, BOOL");
        List<Node> loopChildren = loop.getChildren();
        Node doKeyword = loopChildren.get(0);
        Node openingBrace = loopChildren.get(1);
        Node bodyAlgo = loopChildren.get(2);
        Node closingBrace = loopChildren.get(3);
        Node condNode = loopChildren.get(4);
        Node conditionNode = loopChildren.get(5);

        requireLeafToken(doKeyword, "do", loop);
        requireLeafToken(openingBrace, "{", loop);
        requireKind(bodyAlgo, ALGO);
        requireLeafToken(closingBrace, "}", loop);
        String conditionKind = conditionKind(condNode, loop);
        requireKind(conditionNode, BOOL);

        String startLabel = ctx.newLabel();
        String exitLabel = ctx.newLabel();
        String bodyCode = transAlgo(bodyAlgo, ctx);
        boolean isWhileCondition = conditionKind.equals("while");
        String conditionCode;
        if (isWhileCondition) {
            conditionCode = BoolGen.transBool(conditionNode, startLabel, exitLabel, ctx);
        } else {
            conditionCode = BoolGen.transNot(conditionNode, startLabel, exitLabel, ctx);
        }

        StringBuilder loopCode = new StringBuilder();
        loopCode.append(label(startLabel, "do"));
        loopCode.append(bodyCode);
        loopCode.append(conditionCode);
        loopCode.append(label(exitLabel, "exit"));
        return loopCode.toString();
    }

    private static String conditionKind(Node cond, Node loop) {
        requireKind(cond, COND);
        requireChildCount(cond, 1, "COND requires exactly one 'while' or 'until' token");
        Node token = cond.getChildren().get(0);
        if (!token.isLeaf()) {
            throw malformed(loop, "COND must contain a 'while' or 'until' token leaf");
        }

        String conditionKind = token.getContents();
        boolean isWhileCondition = conditionKind.equals("while");
        boolean isUntilCondition = conditionKind.equals("until");
        boolean isRecognizedCondition = isWhileCondition || isUntilCondition;
        if (!isRecognizedCondition) {
            throw malformed(loop, "COND must contain a 'while' or 'until' token leaf");
        }
        return conditionKind;
    }
}
