package spl.typeanalysis;

/** User-facing categories for type-analysis errors. */
public enum TypeErrorType {
    INCOMPATIBLE_TYPES("Incompatible types"),
    INVALID_FUNCTION_USE("Invalid function use"),
    INCOMPATIBLE_NUMERIC_OPERATIONS("Incompatible numeric operations"),
    INVALID_MODULO_VALUE("Invalid modulo value"),
    INTERNAL_ANALYSIS_ERROR("Internal type-analysis error");

    private final String description;

    TypeErrorType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
