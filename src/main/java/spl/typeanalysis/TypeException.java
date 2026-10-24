package spl.typeanalysis;

import spl.tree.Node;

public class TypeException extends RuntimeException {
    private final TypeErrorType errorType;
    private final Node node;

    public TypeException(TypeErrorType errorType, String message, Node node) {
        super(message);
        this.errorType = errorType;
        this.node = node;
    }

    public TypeErrorType getErrorType() {
        return errorType;
    }

    public String getDescription() {
        return errorType.description();
    }

    /** The offending node (may be null for tree-wide errors). */
    public Node getNode() {
        return node;
    }
}
