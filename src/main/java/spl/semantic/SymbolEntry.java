package spl.semantic;

public class SymbolEntry {
    public enum Kind { VAR, PARAM, FUNC }
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

    public String getOriginalName() {
        return this.originalName; 
    }
    
    public Kind getKind() {
        return this.kind; 
    }
    public int getLevel() { 
        return this.level; 
    }
    
    public int getDeclNodeId() { 
        return this.declNodeId; 
    }
    
    public String getUniqueName() { 
        return this.uniqueName;
    }

    @Override
    public String toString() {
        return uniqueName + " <- " + originalName + " (" + kind + ", level " + level + ", node " + declNodeId + ")";
    }
}
