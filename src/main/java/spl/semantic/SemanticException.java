package spl.semantic;

public class SemanticException extends RuntimeException {
    private final SemanticErrorType errorType;
    private final int nodeId;

    public SemanticException(SemanticErrorType errorType, String message, int nodeId) {
        super(message);
        this.errorType = errorType;
        this.nodeId = nodeId;
    }

    public String getDescription() {
        return errorType.description();
    }

    public int getNodeId() {
        return nodeId;
    }
}
