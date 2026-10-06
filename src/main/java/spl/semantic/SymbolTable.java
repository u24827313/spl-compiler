package spl.semantic;

public class SymbolTable {
    SymbolTable(NameGenerator names){

    }
    NameGenerator names(){
        return null; //stub
    }               // the checkers use this to create new names
    void pushScope(int level){

    }
    void popScope(){

    }
    Scope currentScope(){
        return null; //stub
    }

    SymbolEntry lookupVar(String name){
        return null; //stub
    }   // walks the stack, innermost first

    SymbolEntry lookupFunc(String name){
        return null; //stub
    }   // TOP scope only (no walking outward)

    void record(int nodeId, SymbolEntry e){

    }  // node -> entity, for the code generator

    SymbolEntry getEntry(int nodeId){
        return null; //stub
    }
}
