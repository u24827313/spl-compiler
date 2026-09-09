package spl.tree;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Owner: TODO(assign teammate)
 *
 * Serializes a Node tree to tree.xml per the spec:
 *  - root: id, contents (start symbol), children (id list) -- no "parent" field
 *  - inner node: id, contents (non-terminal), children (id list), parent
 *  - leaf: id, contents (terminal token text), parent (no "children" field)
 *
 * Uses only javax.xml (JDK built-in) — no extra dependency needed.
 */
public class XmlTreeWriter {

    public void write(Node root, File outputFile) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element treeEl = doc.createElement("tree");
        doc.appendChild(treeEl);

        List<Node> allNodes = new ArrayList<>();
        collect(root, allNodes);

        for (Node n : allNodes) {
            Element nodeEl = doc.createElement("node");
            nodeEl.setAttribute("id", String.valueOf(n.id));

            Element contentsEl = doc.createElement("contents");
            contentsEl.setTextContent(n.contents);
            nodeEl.appendChild(contentsEl);

            if (!n.isLeaf()) {
                Element childrenEl = doc.createElement("children");
                StringBuilder ids = new StringBuilder();
                for (Node c : n.children) {
                    if (ids.length() > 0) ids.append(",");
                    ids.append(c.id);
                }
                childrenEl.setTextContent(ids.toString());
                nodeEl.appendChild(childrenEl);
            }

            if (n.parent != null) {
                Element parentEl = doc.createElement("parent");
                parentEl.setTextContent(String.valueOf(n.parent.id));
                nodeEl.appendChild(parentEl);
            }

            treeEl.appendChild(nodeEl);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
    }

    private void collect(Node n, List<Node> out) {
        out.add(n);
        for (Node c : n.children) collect(c, out);
    }
}
