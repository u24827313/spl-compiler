package spl.semantic;

/** User-facing categories for semantic errors. */
public enum SemanticErrorType {
    DUPLICATE_FUNCTION("Duplicate function declaration"),
    DUPLICATE_VARIABLE("Duplicate variable declaration"),
    DUPLICATE_PARAMETER("Duplicate parameter declaration"),
    UNDECLARED_FUNCTION("Undeclared function"),
    UNDECLARED_VARIABLE("Undeclared variable"),
    NAME_CONFLICT("Name conflict"),
    INVALID_LOCAL_VARIABLE_NAME("Invalid local variable name");

    private final String description;

    SemanticErrorType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
