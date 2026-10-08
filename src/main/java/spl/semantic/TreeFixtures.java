package spl.semantic;

import org.w3c.dom.*;
import spl.tree.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.*;

public class TreeFixtures {
    static Node load(Path file) throws Exception{
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
        NodeList xml = doc.getElementsByTagName("node");

        Map<Integer, Node> byId = new HashMap<>();
        Map<Integer, String> childLists = new HashMap<>();
        Node root = null;

        for(int i =0; i < xml.getLength(); i++){
            Element e = (Element) xml.item(i);
            int id = Integer.parseInt(e.getAttribute("id"));
            byId.put(id, new Node(id, text(e, "contents")));
            childLists.put(id, text(e, "children"));
        }

        for(int i =0;i < xml.getLength(); i++){
            Element e = (Element) xml.item(i);
            int id = Integer.parseInt(e.getAttribute("id"));
            if(e.getElementsByTagName("parent").getLength() == 0) root = byId.get(id);
            String list = childLists.get(id);
            if(!list.isEmpty()){
                for(String c: list.split(",")) byId.get(id).addChild(byId.get(Integer.parseInt(c.trim())));
            }
        }
        return root;
    }

    private static String text(Element e, String tag) {
        NodeList l = e.getElementsByTagName(tag);
        return l.getLength() == 0 ? "" : l.item(0).getTextContent();
    }
}
