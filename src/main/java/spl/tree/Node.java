package spl.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Single syntax-tree node. Doubles as inner-node or leaf depending on
 * whether `children` is empty (leaf => contents is a terminal token's
 * lexeme; inner/root => contents is a non-terminal name).
 *
 * id: unique across the whole tree (foreign key for later semantic phases).
 */
public class Node {
    public final int id;
    public final String contents;
    public final List<Node> children = new ArrayList<>();
    public Node parent; // null only for the root

    public Node(int id, String contents) {
        this.id = id;
        this.contents = contents;
    }

    public void addChild(Node child) {
        child.parent = this;
        children.add(child);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}
