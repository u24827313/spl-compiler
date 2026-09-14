package spl.tree;

public class SyntaxTree {
    private final Node root;

    public SyntaxTree(Node root) {
        this.root = root;
    }

    public Node getRoot() {
        return root;
    }
}