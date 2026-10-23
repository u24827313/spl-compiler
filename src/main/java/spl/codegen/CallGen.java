package spl.codegen;

import spl.semantic.SymbolTable;
import spl.tree.Node;

import java.util.ArrayList;
import java.util.List;

import static spl.codegen.GenKinds.*;

public class CallGen {

    /** TERM -> CALL: the callee's return value ends up in resultVar. */
    public static String transCallTerm(Node call, String resultVar, CodeGenContext ctx) {
        if (resultVar == null || resultVar.isBlank()) {
            throw new IllegalArgumentException("Numeric CALL requires a non-empty result variable");
        }
        return inline(call, resultVar, ctx);
    }

    /** INSTR -> CALL: procedures do not produce a continuation value. */
    public static String transCallInstr(Node call, CodeGenContext ctx) {
        return inline(call, null, ctx);
    }

    private static String inline(Node call, String resultVar, CodeGenContext ctx) {
        requireCallNode(call);
        if (ctx == null) {
            throw new IllegalArgumentException("Cannot translate CALL without a code generation context");
        }

        FunctionRegistry registry = ctx.registry();
        SymbolTable symbolTable = ctx.symbolTable();
        FunctionInfo function = registry.forCall(call, symbolTable);
        validateCallMode(function, resultVar, call);
        List<Node> argumentTerms = collectArgumentTerms(call);
        validateArity(call, function, argumentTerms);
        Node bodyAlgo = directChild(function.bodyP, ALGO, "function body P");

        StringBuilder generatedCode = new StringBuilder();
        try {
            ctx.beginInline();
            List<String> argumentTemporaries = captureArguments(argumentTerms, ctx, generatedCode);
            bindParameters(function, argumentTemporaries, ctx, generatedCode);
            String bodyCode = AlgoGen.transAlgo(bodyAlgo, ctx);
            generatedCode.append(bodyCode);
            appendReturnValue(function, resultVar, ctx, generatedCode);
            return generatedCode.toString();
        } finally {
            // beginInline increments before it can throw, so this balances overflow too.
            ctx.endInline();
        }
    }

    private static List<String> captureArguments(List<Node> argumentTerms, CodeGenContext ctx,
                                                  StringBuilder generatedCode) {
        List<String> argumentTemporaries = new ArrayList<>();
        for (Node argumentTerm : argumentTerms) {
            String argumentTemporary = ctx.newVar();
            String argumentCode = TermGen.transTerm(argumentTerm, argumentTemporary, ctx);
            generatedCode.append(argumentCode);
            argumentTemporaries.add(argumentTemporary);
        }
        return argumentTemporaries;
    }

    private static void bindParameters(FunctionInfo function, List<String> argumentTemporaries,
                                       CodeGenContext ctx, StringBuilder generatedCode) {
        for (int parameterIndex = 0; parameterIndex < function.paramNameNodes.size(); parameterIndex++) {
            Node parameterNode = function.paramNameNodes.get(parameterIndex);
            String parameterName = ctx.lookUp(parameterNode);
            String argumentTemporary = argumentTemporaries.get(parameterIndex);
            generatedCode.append(parameterName).append(" = ")
                    .append(argumentTemporary).append(";\n");
        }
    }

    private static void appendReturnValue(FunctionInfo function, String resultVar,
                                          CodeGenContext ctx, StringBuilder generatedCode) {
        if (function.isVoid) {
            return;
        }
        if (function.returnTerm == null) {
            throw new IllegalStateException("Numeric function " + function.uniqueName
                    + " has no return TERM");
        }
        if (resultVar == null) {
            throw new IllegalArgumentException("Numeric function " + function.uniqueName
                    + " cannot be used as a statement");
        }

        String returnTemporary = ctx.newVar(function.uniqueName);
        String returnCode = TermGen.transTerm(function.returnTerm, returnTemporary, ctx);
        generatedCode.append(returnCode);
        generatedCode.append(resultVar).append(" = ").append(returnTemporary).append(";\n");
    }

    private static List<Node> collectArgumentTerms(Node call) {
        Node input = directChild(call, INPUT, "CALL");
        List<Node> argumentTerms = new ArrayList<>();
        Node currentInput = input;
        while (true) {
            requireKind(currentInput, INPUT, "INPUT chain");
            List<Node> inputChildren = currentInput.getChildren();
            if (inputChildren.isEmpty()) {
                break;
            }

            boolean hasExpectedShape = inputChildren.size() == 2
                    && is(inputChildren.get(1), INPUT);
            if (!hasExpectedShape) {
                throw malformed(currentInput, "expected TERM INPUT or epsilon");
            }

            Node argumentTerm = inputChildren.get(0);
            requireKind(argumentTerm, TERM, "CALL argument");
            argumentTerms.add(argumentTerm);
            currentInput = inputChildren.get(1);
        }
        return argumentTerms;
    }

    private static void validateCallMode(FunctionInfo function, String resultVar, Node call) {
        boolean isVoidFunctionUsedAsTerm = function.isVoid && resultVar != null;
        if (isVoidFunctionUsedAsTerm) {
            throw new IllegalArgumentException("Void procedure " + function.uniqueName
                    + " cannot be used as a TERM at CALL node " + call.getId());
        }

        boolean isNumericFunctionUsedAsInstruction = !function.isVoid && resultVar == null;
        if (isNumericFunctionUsedAsInstruction) {
            throw new IllegalArgumentException("Numeric function " + function.uniqueName
                    + " cannot be used as an INSTR at CALL node " + call.getId());
        }
    }

    private static void validateArity(Node call, FunctionInfo function, List<Node> argumentTerms) {
        int expectedCount = function.paramNameNodes.size();
        int actualCount = argumentTerms.size();
        if (expectedCount != actualCount) {
            throw new IllegalArgumentException("Arity mismatch for function " + function.uniqueName
                    + " at CALL node " + call.getId() + ": expected " + expectedCount
                    + " arguments but found " + actualCount);
        }
    }

    private static Node directChild(Node parent, String expectedKind, String parentDescription) {
        Node foundChild = null;
        for (Node child : parent.getChildren()) {
            if (!is(child, expectedKind)) {
                continue;
            }
            if (foundChild != null) {
                throw malformed(parent, "expected one direct " + expectedKind + " child");
            }
            foundChild = child;
        }
        if (foundChild == null) {
            throw malformed(parent, "missing direct " + expectedKind + " child in "
                    + parentDescription);
        }
        return foundChild;
    }

    private static void requireCallNode(Node call) {
        requireKind(call, CALL, "CALL");
        List<Node> children = call.getChildren();
        if (children.size() != 4) {
            throw malformed(call, "expected NAME '(' INPUT ')' with four direct children");
        }

        Node functionName = children.get(0);
        Node openingParenthesis = children.get(1);
        Node input = children.get(2);
        Node closingParenthesis = children.get(3);
        boolean hasName = isUserDefinedName(functionName);
        boolean hasOpeningParenthesis = is(openingParenthesis, "(");
        boolean hasInput = is(input, INPUT);
        boolean hasClosingParenthesis = is(closingParenthesis, ")");
        boolean hasValidShape = hasName && hasOpeningParenthesis && hasInput && hasClosingParenthesis;
        if (!hasValidShape) {
            throw malformed(call, "expected NAME '(' INPUT ')' with four direct children");
        }
    }

    private static void requireKind(Node node, String expectedKind, String description) {
        if (!is(node, expectedKind)) {
            String actualKind = "null";
            if (node != null) {
                actualKind = "'" + node.getContents() + "' at node " + node.getId();
            }
            throw new IllegalArgumentException("Expected " + description + " " + expectedKind
                    + " but found " + actualKind);
        }
    }

    private static IllegalArgumentException malformed(Node node, String detail) {
        return new IllegalArgumentException("Malformed " + node.getContents() + " node "
                + node.getId() + ": " + detail);
    }

}
