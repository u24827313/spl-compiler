package spl.semantic;

import spl.tree.Node;
import java.util.List;
import static spl.semantic.NodeKinds.*;
public class SemanticAnalyzer  {
    private final VariableChecker vars;
    private final FunctionChecker funcs;
    private SymbolTable table;

    public SemanticAnalyzer() { this(new VariableChecker(), new FunctionChecker()); }
    SemanticAnalyzer(VariableChecker vars, FunctionChecker funcs) {   
        this.vars = vars;
        this.funcs = funcs;
    }

    public SymbolTable analyse(Node root) {
        return analyse(root, new SymbolTable(new NameGenerator()));
    }

    SymbolTable analyse(Node root, SymbolTable table) {             
        this.table = table;
        analyseP(childNamed(root, P), 0, null);
        return table;
    }

    private void analyseP(Node p, int level, Node enclosingFType){
        table.pushScope(level);
        if(enclosingFType != null){
            for(Node n: namesInVDeclChain(childNamed(enclosingFType, V_DECL))){
                vars.declareParameter(n, table);
            }
        }

        for(Node n: namesInVDeclChain(childNamed(p, V_DECL))){
            vars.declareVariable(n, table);
        }

        List<Node> fTypes = fTypesInFDeclChain(childNamed(p, F_DECL));
        for(Node ft: fTypes){
            funcs.declareFunction(firstNameChild(ft), table);
        }

        crawlUsages(childNamed(p, ALGO));                                         // 4
        if (enclosingFType != null)
            crawlUsages(childNamed(enclosingFType, TERM));                        //   return TERM, still in scope

        for (Node ft : fTypes)                                                    // 5
            analyseP(childNamed(ft, P), level + 1, ft);

        table.popScope();  
    }

    private void crawlUsages(Node n){
        if(n == null) return;
        if(is(n, CALL)){
            Node name = firstNameChild(n);
            funcs.resolveFunctionCall(name, table);
            for(Node c: n.getChildren()){
                if(c != name) crawlUsages(c);
            }
            return;
        }
        if(isUserDefinedName(n)){
            vars.resolveVariableUse(n, table);
            return;
        }
        for(Node c: n.getChildren()) crawlUsages(c);
    }
}
