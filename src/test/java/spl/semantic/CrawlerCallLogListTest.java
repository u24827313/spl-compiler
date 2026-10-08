package spl.semantic;

import org.junit.jupiter.api.Test;
import spl.tree.Node;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CrawlerCallLogListTest {

    static String fmt(String method, Node n) {
        return method + " " + n.getContents() + " (node " + n.getId() + ")";
    }

    static class RecVars extends VariableChecker {
        final List<String> log;
        RecVars(List<String> log) { this.log = log; }
        @Override void declareParameter(Node n, SymbolTable t)    { log.add(fmt("declareParameter", n)); }
        @Override void declareVariable(Node n, SymbolTable t)     { log.add(fmt("declareVariable", n)); }
        @Override void resolveVariableUse(Node n, SymbolTable t)  { log.add(fmt("resolveVariableUse", n)); }
    }

    static class RecFuncs extends FunctionChecker {
        final List<String> log;
        RecFuncs(List<String> log) { this.log = log; }
        @Override void declareFunction(Node n, SymbolTable t)     { log.add(fmt("declareFunction", n)); }
        @Override void resolveFunctionCall(Node n, SymbolTable t) { log.add(fmt("resolveFunctionCall", n)); }
    }

    static class RecTable extends SymbolTable {
        final List<String> log;
        RecTable(List<String> log) { super(new NameGenerator()); this.log = log; }
        @Override void pushScope(int level) { log.add("pushScope " + level); }
        @Override void popScope()           { log.add("popScope"); }
    }

    @Test
    void sampleTreeCallSequence() throws Exception {
        Node root = TreeFixtures.load(Path.of("tree.xml"));
        List<String> log = new ArrayList<>();

        new SemanticAnalyzer(new RecVars(log), new RecFuncs(log))
            .analyse(root, new RecTable(log));

        List<String> expected = Files.readAllLines(
            Path.of("src/test/java/spl/semantic/sample.calllog.txt")
        );
        assertEquals(expected, log);
    }
}