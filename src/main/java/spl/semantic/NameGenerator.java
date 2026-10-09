package spl.semantic;

public class NameGenerator { 
    private static final String VARIABLE_PREFIX = "v";
    private static final String FUNCTION_PREFIX = "f";

    private int variableCounter = 0;
    private int functionCounter = 0;

    String next(SymbolEntry.Kind kind){

        switch(kind) {
            case VAR:
            case PARAM:
                return VARIABLE_PREFIX + (++variableCounter);
            case FUNC:
                return FUNCTION_PREFIX + (++functionCounter);
            default:
                throw new IllegalArgumentException(
                    "Unsupported symbol kind: " + kind
                );
        }
    }
}
