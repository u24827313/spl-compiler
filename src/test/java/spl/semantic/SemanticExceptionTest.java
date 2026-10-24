package spl.semantic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SemanticExceptionTest {

    @Test
    void preservesErrorMessageAndNodeId() {
        SemanticException exception = new SemanticException(
                SemanticErrorType.UNDECLARED_FUNCTION,
                "The function '#foo' has not been declared in the current scope.",
                42
        );

        assertEquals(42, exception.getNodeId());

        assertEquals("Undeclared function", exception.getDescription());
        assertTrue(exception.getMessage().contains("#foo"));
        assertFalse(exception.getMessage().contains("node 42"));
    }
}