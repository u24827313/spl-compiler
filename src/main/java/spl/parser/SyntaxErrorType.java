package spl.parser;

/** User-facing categories for syntax errors. */
public enum SyntaxErrorType {
    MISSING_TERM_OR_EXPRESSION("Missing term or expression"),
    MISSING_PUNCTUATION("Missing punctuation"),
    INVALID_PRINT_OPERAND("Invalid print operand"),
    EXPECTED_ASSIGNMENT_OR_FUNCTION_CALL("Expected assignment or function call after a name"),
    INVALID_BOOLEAN_EXPRESSION("Invalid boolean expression"),
    MISSING_BRANCH_KEYWORD("Missing branch keyword"),
    MISSING_CLOSING_DELIMITER("Missing closing delimiter"),
    MISSING_PROGRAM_SECTION_SEPARATOR("Missing program-section separator"),
    INVALID_POST_TEST_LOOP_CONDITION("Invalid post-test loop condition"),
    INVALID_INSTRUCTION("Invalid instruction"),
    INVALID_FUNCTION_DECLARATION("Invalid function declaration"),
    UNEXPECTED_SYNTAX("Unexpected syntax");

    private final String description;

    SyntaxErrorType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
