package spl.typeanalysis;

public enum Type {
    UNKNOWN,
    NUMERIC,
    BOOLEAN,
    PROCEDURE,
    OK;
 
    /** Lower-case form used in error messages, e.g. "numeric". */
    public String label() {
        return name().toLowerCase();
    }
}
