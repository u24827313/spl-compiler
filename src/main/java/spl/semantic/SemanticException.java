package spl.semantic;

public class SemanticException extends RuntimeException {
    private final int nodeId;

    SemanticException(String message, int nodeId){
        super(message + " (node " + nodeId + ")");
        this.nodeId = nodeId;
    }

    public int getNodeId(){
        return nodeId;
    }
}
