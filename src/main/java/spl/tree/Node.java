package spl.tree;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private final int id;
    private final String contents;
    private final List<Node> children = new ArrayList<>();
    private Node parent; // null for the root

    public Node(int id, String contents) {
        this.id = id;
        this.contents = contents;
    }

    public int getId() { return id; }
    public String getContents() { return contents; }
    public List<Node> getChildren() { return children; }
    public Node getParent() { return parent; }

    public void addChild(Node child) {
        children.add(child);
        child.parent = this;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}