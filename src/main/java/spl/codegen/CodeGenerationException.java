package spl.codegen;

import spl.tree.Node;

/** A user-facing error encountered while generating target code. */
public class CodeGenerationException extends IllegalArgumentException {
    private final Node node;

    public CodeGenerationException(String message, Node node) {
        super(message);
        this.node = node;
    }

    public String getDescription() {
        return "Incorrect number of function arguments";
    }

    public Node getNode() {
        return node;
    }
}
