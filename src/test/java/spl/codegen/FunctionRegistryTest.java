package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.tree.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionRegistryTest {

    @Test
    void registersVoidAndNumericFunctionMetadata() {
        String source = ": void #show ( #value ) { : : print ( #value ) ; return } "
                + "num #double ( #number ) { : : return ( mul ( #number 2 ) ) } "
                + ": #show ( 3 ) ; print ( #double ( 4 ) ) ; $ ";
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(source);
        Node voidName = CallGenTestSupport.findByText(program.root(), "#show");
        Node numericName = CallGenTestSupport.findByText(program.root(), "#double");
        FunctionInfo voidInfo = program.registry().forCall(
                CallGenTestSupport.findFirst(program.root(), "CALL"), program.symbols());

        Node numericCall = findCallByName(program.root(), "#double");
        FunctionInfo numericInfo = program.registry().forCall(numericCall, program.symbols());

        assertNotNull(voidName);
        assertNotNull(numericName);
        assertTrue(voidInfo.isVoid);
        assertEquals("f1", voidInfo.uniqueName);
        assertEquals(1, voidInfo.paramNameNodes.size());
        assertNull(voidInfo.returnTerm);
        assertNotNull(voidInfo.bodyP);

        assertEquals("f2", numericInfo.uniqueName);
        assertEquals(1, numericInfo.paramNameNodes.size());
        assertNotNull(numericInfo.returnTerm);
        assertEquals("TERM", numericInfo.returnTerm.getContents());
    }

    @Test
    void discoversNestedDeclarationsAndKeepsParameterListsSeparateFromLocals() {
        String source = ": num #outer ( #argument ) { : num #inner ( #parameter ) "
                + "{ #local : : #local = #parameter ; return ( #local ) } "
                + ": return ( #inner ( #argument ) ) } "
                + ": print ( #outer ( 5 ) ) ; $ ";
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(source);

        Node innerCall = findCallByName(program.root(), "#inner");
        FunctionInfo innerInfo = program.registry().forCall(innerCall, program.symbols());

        assertEquals(1, innerInfo.paramNameNodes.size());
        assertEquals("#parameter", innerInfo.paramNameNodes.get(0).getContents());
        assertEquals("#local", CallGenTestSupport.findByText(innerInfo.bodyP, "#local").getContents());
    }

    @Test
    void buildsEmptyRegistryForAProgramWithoutFunctions() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(": : print \"ok\" ; $ ");
        FunctionRegistry registry = FunctionRegistry.build(program.root(), program.symbols());

        assertNotNull(registry);
    }

    @Test
    void repeatedLookupsReturnTheRegisteredFunctionInfo() {
        String source = ": num #id ( #x ) { : : return ( #x ) } : print ( #id ( 4 ) ) ; $ ";
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(source);
        Node call = findCallByName(program.root(), "#id");

        FunctionInfo firstLookup = program.registry().forCall(call, program.symbols());
        FunctionInfo secondLookup = program.registry().forCall(call, program.symbols());

        assertSame(firstLookup, secondLookup);
    }

    @Test
    void rejectsWrongRootAndMissingCallMetadataWithContext() {
        assertThrows(IllegalArgumentException.class,
                () -> FunctionRegistry.build(new Node(990_001, "P"), null));

        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(": : print \"ok\" ; $ ");
        Node malformedCall = new Node(990_002, "CALL");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> program.registry().forCall(malformedCall, program.symbols()));
        assertTrue(failure.getMessage().contains("990002"));
    }

    private static Node findCallByName(Node root, String name) {
        if ("CALL".equals(root.getContents())) {
            for (Node child : root.getChildren()) {
                if (name.equals(child.getContents())) {
                    return root;
                }
            }
        }
        for (Node child : root.getChildren()) {
            Node match = findCallByName(child, name);
            if (match != null) {
                return match;
            }
        }
        return null;
    }
}
