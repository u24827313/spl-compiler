package spl.semantic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SemanticExceptionTest {

    @Test
    void preservesErrorMessageAndNodeId() {
        SemanticException exception =
            new SemanticException("Undeclared function '#foo'", 42);

        assertEquals(42, exception.getNodeId());

        assertTrue(
            exception.getMessage().contains("Undeclared function '#foo'")
        );

        assertTrue(
            exception.getMessage().contains("node 42")
        );
    }
}