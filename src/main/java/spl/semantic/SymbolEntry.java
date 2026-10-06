package spl.semantic;

public class SymbolEntry {
    enum Kind { VAR, PARAM, FUNC }
    String originalName; 
    Kind kind; 
    int level; 
    int declNodeId; 
    String uniqueName;

    SymbolEntry(String originalName, Kind kind, int level, int declNodeId, String uniqueName) {
        this.originalName = originalName;
        this.kind = kind;
        this.level = level;
        this.declNodeId = declNodeId;
        this.uniqueName = uniqueName;
    }

    @Override
    public String toString() {
        return uniqueName + " <- " + originalName + " (" + kind + ", level " + level + ", node " + declNodeId + ")";
    }
}
