package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.tree.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallGenTest {

    @Test
    void inlinesZeroArgumentProcedureWithoutReturnStorage() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": void #show ( ) { : : print \"hi\" ; return } : #show ( ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#show");

        String code = CallGen.transCallInstr(call, program.context());

        assertTrue(code.contains("std::cout << \"hi\";\n"));
        assertFalse(code.contains(" = null;"));
        assertFalse(program.context().declarations().contains("f1t"));
    }

    @Test
    void capturesAllArgumentsBeforeBindingAnyProcedureParameter() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": void #show ( #left #right ) { : : print ( #left ) ; print ( #right ) ; return } "
                        + ": #show ( add ( 1 2 ) mul ( 3 4 ) ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#show");
        FunctionInfo function = program.registry().forCall(call, program.symbols());

        String code = CallGen.transCallInstr(call, program.context());

        int firstArgumentFinish = code.indexOf("t1 = t2 + t3;");
        int secondArgumentStart = code.indexOf("t5 = 3;");
        int firstBinding = code.indexOf(program.context().lookUp(function.paramNameNodes.get(0)) + " = t1;");
        int secondBinding = code.indexOf(program.context().lookUp(function.paramNameNodes.get(1)) + " = t4;");
        int bodyRead = code.indexOf("std::cout <<");

        assertTrue(firstArgumentFinish >= 0);
        assertTrue(secondArgumentStart > firstArgumentFinish);
        assertTrue(firstBinding > secondArgumentStart);
        assertTrue(secondBinding > firstBinding);
        assertTrue(bodyRead > secondBinding);
    }

    @Test
    void dispatchesStatementCallsThroughInstrAndAlgo() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": void #show ( ) { : : print \"inside\" ; return } "
                        + ": #show ( ) ; print \"outside\" ; $ ");
        Node mainProgram = CallGenTestSupport.directChild(program.root(), "P");
        Node mainAlgo = CallGenTestSupport.directChild(mainProgram, "ALGO");

        String code = AlgoGen.transAlgo(mainAlgo, program.context());

        assertTrue(code.indexOf("inside") < code.indexOf("outside"));
        assertTrue(code.endsWith("/* end */\n"));
    }

    @Test
    void rejectsArityMismatchBeforeAllocatingTemporaries() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": void #show ( #value ) { : : print ( #value ) ; return } : #show ( ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#show");

        CodeGenerationException failure = assertThrows(
                CodeGenerationException.class,
                () -> CallGen.transCallInstr(call, program.context())
        );

        assertEquals("Incorrect number of function arguments", failure.getDescription());
        assertEquals(
                "The function '#show' expects 1 argument, but this call provides 0 arguments.",
                failure.getMessage()
        );
        assertEquals("", program.context().declarations());
    }

    @Test
    void rejectsWrongCallModeAndMissingRegistryClearly() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": void #show ( ) { : : return } : #show ( ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#show");

        IllegalArgumentException modeError = assertThrows(IllegalArgumentException.class,
                () -> CallGen.transCallTerm(call, "result", program.context()));
        IllegalStateException contextError = assertThrows(IllegalStateException.class,
                () -> CallGen.transCallInstr(call,
                        new CodeGenContext(program.symbols(), "int")));

        assertTrue(modeError.getMessage().contains("Void procedure"));
        assertTrue(contextError.getMessage().contains("FunctionRegistry"));
    }

    @Test
    void numericFunctionStoresItsReturnInFreshPrefixedStorageThenConnectsCaller() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": num #double ( #value ) { : : return ( mul ( #value 2 ) ) } "
                        + ": print ( #double ( 4 ) ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#double");

        String code = CallGen.transCallTerm(call, "answer", program.context());
        assertTrue(code.contains("t1 = 4;\n"));
        assertTrue(code.contains("v1 = t1;\n"));
        assertTrue(code.contains("t3 = v1;\n"));
        assertTrue(code.contains("t4 = 2;\n"));
        assertTrue(code.contains("f1t2 = t3 * t4;\n"));
        assertTrue(code.endsWith("answer = f1t2;\n"));
        assertTrue(program.context().declarations().contains("int f1t2;\n"));
    }

    @Test
    void numericReturnExpressionRunsAfterTheFunctionBody() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": num #increment ( #value ) { #local : : #local = add ( #value 1 ) ; "
                        + "return ( #local ) } : print ( #increment ( 4 ) ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#increment");

        String code = CallGen.transCallTerm(call, "answer", program.context());

        int bodyAssignment = code.indexOf("v2 = t");
        int returnRead = code.indexOf("f1t");
        int callerCopy = code.lastIndexOf("answer = f1t");
        assertTrue(bodyAssignment >= 0);
        assertTrue(returnRead > bodyAssignment);
        assertTrue(callerCopy > returnRead);
    }

    @Test
    void rejectsBlankNumericDestinationAndNumericStatementCall() {
        CallGenTestSupport.AnalysedProgram program = CallGenTestSupport.analyse(
                ": num #identity ( ) { : : return ( 7 ) } : print ( #identity ( ) ) ; $ ");
        Node call = CallGenTestSupport.findCallByName(program.root(), "#identity");

        assertThrows(IllegalArgumentException.class,
                () -> CallGen.transCallTerm(call, " ", program.context()));
        assertThrows(IllegalArgumentException.class,
                () -> CallGen.transCallInstr(call, program.context()));
    }

}
