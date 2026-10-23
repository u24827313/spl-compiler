package spl.codegen;

import spl.semantic.SymbolEntry;
import spl.semantic.SymbolTable;
import spl.tree.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spl.codegen.GenKinds.*;

final class FunctionRegistry {

    /** key = id of the #name leaf in the F_TYPE (= SymbolEntry.getDeclNodeId()). */
    private final Map<Integer, FunctionInfo> byDeclNodeId = new HashMap<>();

    FunctionRegistry() {}

    /** Crawl the whole SPL_PROG tree and register every F_TYPE. */
    static FunctionRegistry build(Node splProgRoot, SymbolTable symT) {
        requireNodeKind(splProgRoot, "SPL_PROG", "registry root");
        if (symT == null) {
            throw new IllegalArgumentException("Cannot build function registry without a symbol table");
        }

        FunctionRegistry registry = new FunctionRegistry();
        registry.registerTree(splProgRoot, symT);
        return registry;
    }

    /** The FunctionInfo of the function that this CALL node invokes. */
    FunctionInfo forCall(Node callNode, SymbolTable symT) {
        requireNodeKind(callNode, CALL, "call node");
        if (symT == null) {
            throw new IllegalArgumentException("Cannot resolve CALL without a symbol table");
        }

        Node callName = firstNameChild(callNode);
        if (callName == null) {
            throw malformed(callNode, "expected a direct function NAME child");
        }

        SymbolEntry callEntry = symT.getEntry(callName.getId());
        if (callEntry == null) {
            throw new IllegalStateException("No symbol-table entry for CALL name node "
                    + callName.getId() + " (" + callName.getContents() + ")");
        }
        if (callEntry.getKind() != SymbolEntry.Kind.FUNC) {
            throw new IllegalStateException("CALL name node " + callName.getId()
                    + " resolves to " + callEntry.getKind() + ", not a function");
        }

        int declarationNodeId = callEntry.getDeclNodeId();
        FunctionInfo functionInfo = byDeclNodeId.get(declarationNodeId);
        if (functionInfo == null) {
            throw new IllegalStateException("No registered F_TYPE for CALL name node "
                    + callName.getId() + "; resolved declaration NAME node " + declarationNodeId);
        }
        return functionInfo;
    }

    private void registerTree(Node node, SymbolTable symT) {
        if (is(node, F_TYPE)) {
            registerFunction(node, symT);
        }

        for (Node child : node.getChildren()) {
            registerTree(child, symT);
        }
    }

    private void registerFunction(Node functionType, SymbolTable symT) {
        List<Node> children = functionType.getChildren();
        if (children.isEmpty()) {
            throw malformed(functionType, "missing function header");
        }

        String functionKind = children.get(0).getContents();
        boolean isVoid;
        if ("void".equals(functionKind)) {
            isVoid = true;
        } else if ("num".equals(functionKind)) {
            isVoid = false;
        } else {
            throw malformed(functionType, "expected 'void' or 'num' header token");
        }

        Node functionName = uniqueDirectName(functionType);
        Node parameterDeclaration = uniqueDirectChild(functionType, V_DECL);
        Node bodyProgram = uniqueDirectChild(functionType, P);
        List<Node> returnTerms = childrenNamed(functionType, TERM);

        if (isVoid && !returnTerms.isEmpty()) {
            throw malformed(functionType, "void function must not contain a return TERM");
        }
        if (!isVoid && returnTerms.size() != 1) {
            throw malformed(functionType, "numeric function must contain exactly one direct return TERM");
        }

        SymbolEntry declarationEntry = symT.getEntry(functionName.getId());
        if (declarationEntry == null) {
            throw new IllegalStateException("No symbol-table entry for function declaration NAME node "
                    + functionName.getId() + " (" + functionName.getContents() + ")");
        }
        if (declarationEntry.getKind() != SymbolEntry.Kind.FUNC) {
            throw new IllegalStateException("Function declaration NAME node " + functionName.getId()
                    + " resolves to " + declarationEntry.getKind() + ", not FUNC");
        }
        if (declarationEntry.getDeclNodeId() != functionName.getId()) {
            throw new IllegalStateException("Function declaration entry for NAME node "
                    + functionName.getId() + " points to declaration node "
                    + declarationEntry.getDeclNodeId());
        }

        List<Node> parameterNames = namesInVDecl(parameterDeclaration);
        validateParameterChain(parameterDeclaration, parameterNames, functionType);
        Node returnTerm = null;
        if (!isVoid) {
            returnTerm = returnTerms.get(0);
        }

        FunctionInfo info = new FunctionInfo(declarationEntry.getUniqueName(), isVoid,
                List.copyOf(parameterNames), bodyProgram, returnTerm);
        FunctionInfo previousInfo = byDeclNodeId.putIfAbsent(functionName.getId(), info);
        if (previousInfo != null) {
            throw malformed(functionType, "duplicate function declaration NAME node id "
                    + functionName.getId());
        }
    }

    private static Node uniqueDirectName(Node functionType) {
        List<Node> names = new ArrayList<>();
        for (Node child : functionType.getChildren()) {
            if (isUserDefinedName(child)) {
                names.add(child);
            }
        }
        if (names.size() != 1) {
            throw malformed(functionType, "expected exactly one direct function NAME child");
        }
        return names.get(0);
    }

    private static Node uniqueDirectChild(Node parent, String childKind) {
        List<Node> matchingChildren = childrenNamed(parent, childKind);
        if (matchingChildren.size() != 1) {
            throw malformed(parent, "expected exactly one direct " + childKind
                    + " child but found " + matchingChildren.size());
        }
        return matchingChildren.get(0);
    }

    private static void validateParameterChain(Node parameterDeclaration, List<Node> names,
                                               Node functionType) {
        Node current = parameterDeclaration;
        int foundNames = 0;
        while (true) {
            requireNodeKind(current, V_DECL, "parameter declaration chain");
            List<Node> children = current.getChildren();
            if (children.isEmpty()) {
                boolean hasExpectedNameCount = foundNames == names.size();
                if (!hasExpectedNameCount) {
                    throw malformed(functionType, "inconsistent parameter declaration chain");
                }
                return;
            }

            boolean hasExpectedRecursiveShape = children.size() == 2
                    && isUserDefinedName(children.get(0))
                    && is(children.get(1), V_DECL);
            if (!hasExpectedRecursiveShape) {
                throw malformed(functionType, "malformed V_DECL parameter chain at node "
                        + current.getId());
            }

            foundNames++;
            current = children.get(1);
        }
    }

    private static void requireNodeKind(Node node, String expectedKind, String description) {
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
