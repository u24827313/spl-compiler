package spl.typeanalysis;

import spl.tree.Node;

public class TypeException extends RuntimeException {
    private final Node node;
 
    public TypeException(String message, Node node) {
        super(message);
        this.node = node;
    }
 
    /** The offending node (may be null for tree-wide errors). */
    public Node getNode() {
        return node;
    }
}
