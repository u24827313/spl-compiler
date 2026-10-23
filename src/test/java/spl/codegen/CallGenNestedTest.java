package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.tree.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallGenNestedTest {

    @Test
    void evaluatesNestedSameFunctionArgumentsBeforeBindingOuterParameters() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": num #f ( #a #b ) { : : return ( add ( #a #b ) ) } "
                        + ": print ( #f ( 1 #f ( 2 3 ) ) ) ; $ ");
        Node outerCall = CallGenTestSupport.findCallByName(program.root(), "#f");
        FunctionInfo function = program.registry().forCall(outerCall, program.symbols());
        String firstParameter = program.context().lookUp(function.paramNameNodes.get(0));
        String secondParameter = program.context().lookUp(function.paramNameNodes.get(1));

        String generatedCode = CallGen.transCallTerm(outerCall, "answer", program.context());

        int innerReturnConnection = generatedCode.indexOf("t2 = f1t");
        int outerFirstBinding = generatedCode.lastIndexOf(firstParameter + " = t1;");
        int outerSecondBinding = generatedCode.lastIndexOf(secondParameter + " = t2;");
        int callerResultAssignment = generatedCode.lastIndexOf("answer = ");

        assertTrue(innerReturnConnection >= 0, generatedCode);
        assertTrue(outerFirstBinding > innerReturnConnection, generatedCode);
        assertTrue(outerSecondBinding > outerFirstBinding, generatedCode);
        assertTrue(callerResultAssignment > outerSecondBinding, generatedCode);
        String callerAssignment = generatedCode.substring(callerResultAssignment);
        String expectedAssignmentPattern = "answer = " + function.uniqueName + "t\\d+;\\n";
        assertTrue(callerAssignment.matches(expectedAssignmentPattern), generatedCode);
    }

    @Test
    void registryDistinguishesSameSpelledNestedFunctionsByDeclarationIdentity() {
        String source = ": num #left ( ) { : num #helper ( ) { : : return ( 11 ) } "
                + ": return ( #helper ( ) ) } "
                + "num #right ( ) { : num #helper ( ) { : : return ( 22 ) } "
                + ": return ( #helper ( ) ) } "
                + ": print ( #left ( ) ) ; print \",\" ; print ( #right ( ) ) ; $ ";
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(source);
        Node leftHelperCall = findCallInsideFunction(program.root(), "#left", "#helper");
        Node rightHelperCall = findCallInsideFunction(program.root(), "#right", "#helper");
        FunctionInfo leftHelper = program.registry().forCall(leftHelperCall, program.symbols());
        FunctionInfo rightHelper = program.registry().forCall(rightHelperCall, program.symbols());

        assertNotEquals(leftHelper.uniqueName, rightHelper.uniqueName);
        assertTrue(leftHelper.uniqueName.startsWith("f"));
        assertTrue(rightHelper.uniqueName.startsWith("f"));
    }

    @Test
    void repeatedInlineExpansionsAllocateDisjointLoopLabels() {
        String source = ": num #count ( #limit ) { #i : : #i = 0 ; "
                + "while lesser ( #i #limit ) do { #i = add ( #i 1 ) ; } ; return ( #i ) } "
                + ": print ( #count ( 2 ) ) ; print \",\" ; print ( #count ( 3 ) ) ; $ ";
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(source);
        Node main = CallGenTestSupport.directChild(program.root(), "P");
        Node mainAlgo = CallGenTestSupport.directChild(main, "ALGO");

        String generatedCode = AlgoGen.transAlgo(mainAlgo, program.context());

        assertEquals(java.util.List.of(), CodeGenE2ETest.duplicateLabels(generatedCode));
        assertEquals(java.util.List.of(), CodeGenE2ETest.undefinedGotoTargets(generatedCode));
        assertTrue(generatedCode.contains("goto L"));
    }

    @Test
    void callTranslationDoesNotMutateOriginalTreeIdentity() {
        String source = ": num #id ( #value ) { : : return ( #value ) } "
                + ": print ( #id ( 7 ) ) ; $ ";
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(source);
        Node call = CallGenTestSupport.findCallByName(program.root(), "#id");
        Node originalParent = call.getParent();
        int originalChildren = call.getChildren().size();
        String originalName = call.getChildren().get(0).getContents();

        CallGen.transCallTerm(call, "firstResult", program.context());
        CallGen.transCallTerm(call, "secondResult", program.context());

        assertEquals(originalParent, call.getParent());
        assertEquals(originalChildren, call.getChildren().size());
        assertEquals(originalName, call.getChildren().get(0).getContents());
    }

    private static Node findCallInsideFunction(Node root, String parentName, String calledName) {
        Node parentFunction = CallGenTestSupport.findByText(root, parentName);
        Node parentFunctionType = parentFunction.getParent();
        Node match = CallGenTestSupport.findCallByName(parentFunctionType, calledName);
        if (match != null) {
            return match;
        }
        throw new AssertionError("Missing call to " + calledName + " inside " + parentName);
    }

}
