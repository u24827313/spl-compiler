package spl.semantic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spl.lexer.Lexer;
import spl.parser.Parser;
import spl.tree.Node;
import spl.tree.XmlTreeWriter;
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
    void sampleTreeCallSequence(@TempDir Path tempDir) throws Exception {
        Node parsed = new Parser().parse(new Lexer().tokenize(Files.readString(Path.of("src/SPL.txt")))).getRoot();
        Path xml = tempDir.resolve("tree.xml");
        new XmlTreeWriter().write(parsed, xml.toFile());
        Node root = TreeFixtures.load(xml);
        List<String> log = new ArrayList<>();

        new SemanticAnalyzer(new RecVars(log), new RecFuncs(log))
            .analyse(root, new RecTable(log));

        List<String> expected = Files.readAllLines(
            Path.of("src/test/java/spl/semantic/sample.calllog.txt")
        );
        assertEquals(expected, log);
    }
}