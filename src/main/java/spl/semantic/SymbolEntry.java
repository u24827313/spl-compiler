package spl.semantic;

public class SymbolEntry {
    enum Kind { VAR, PARAM, FUNC }
    String originalName; 
    Kind kind; 
    int level; 
    int declNodeId; 
    String uniqueName;
}
