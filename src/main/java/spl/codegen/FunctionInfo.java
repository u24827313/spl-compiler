package spl.codegen;

import spl.tree.Node;

import java.util.List;

final class FunctionInfo {

    final String uniqueName;

    final boolean isVoid;

    final List<Node> paramNameNodes;

    final Node bodyP;

    final Node returnTerm;

    FunctionInfo(String uniqueName, boolean isVoid, List<Node> paramNameNodes,
                 Node bodyP, Node returnTerm) {
        this.uniqueName = uniqueName;
        this.isVoid = isVoid;
        this.paramNameNodes = paramNameNodes;
        this.bodyP = bodyP;
        this.returnTerm = returnTerm;
    }
}