package spl.semantic;
import spl.tree.Node;
import java.util.ArrayList;
import java.util.List;

public class NodeKinds {
    static final String P = "P", V_DECL = "V_DECL", F_DECL = "F_DECL", F_TYPE = "F_TYPE",
                        ALGO = "ALGO", TERM = "TERM", CALL = "CALL";

    private NodeKinds(){}
    static boolean is(Node n, String contents){
        return n.getContents().equals(contents);
    }


    static boolean isUserDefinedName(Node n){
        return n.isLeaf() && n.getContents().startsWith("#");
    }

    static Node childNamed(Node n, String contents){
        if (n == null) return null;
        for (Node c : n.getChildren()) {
            if (is(c, contents)) return c;
        }
        return null;
    }

    static Node firstNameChild(Node n){
        for(Node c: n.getChildren()) if(isUserDefinedName(c)) return c;
        return null;
    }

    static List<Node> namesInVDeclChain(Node vdecl){
        List<Node> names = new ArrayList<>();

        Node cur = vdecl;
        while(cur != null && !cur.isLeaf()){
            Node next = null;
            for(Node c: cur.getChildren()){
                if(isUserDefinedName(c)) names.add(c);
                else if(is(c, V_DECL)) next = c;
            }
            cur = next;
        }
        return names;
    }

    static List<Node> fTypesInFDeclChain(Node fdecl) {
        List<Node> result = new ArrayList<>();
        Node cur = fdecl;
        while (cur != null && !cur.isLeaf()) {
            Node next = null;
            for (Node c : cur.getChildren()) {
                if (is(c, F_TYPE)) result.add(c);
                else if (is(c, F_DECL)) next = c;
            }
            cur = next;
        }
        return result;
    }
}
