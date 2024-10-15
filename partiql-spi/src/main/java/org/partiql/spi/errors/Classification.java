package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.PEnum;

/**
 * TODO
 */
public final class Classification extends PEnum {

    private Classification(int code) {
        super(code);
    }

    /**
     * TODO
     */
    public static final int UNKNOWN = 0;

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public static Classification UNKNOWN() {
        return new Classification(UNKNOWN);
    }

    /**
     * TODO
     */
    public static final int SYNTAX = 1;

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public static Classification SYNTAX() {
        return new Classification(SYNTAX);
    }

    /**
     * TODO
     */
    public static final int SEMANTIC = 2;

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public static Classification SEMANTIC() {
        return new Classification(SEMANTIC);
    }

    /**
     * TODO
     */
    public static final int COMPILATION = 3;

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public static Classification COMPILATION() {
        return new Classification(COMPILATION);
    }

    /**
     * TODO
     */
    public static final int EXECUTION = 4;

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public static Classification EXECUTION() {
        return new Classification(EXECUTION);
    }

    /**
     * This is subject to change without prior notice.
     * @return a string representation of {@link Classification}, for debugging purposes only
     */
    @Override
    public String toString() {
        int code = code();
        switch (code) {
            case UNKNOWN:
                return "UNKNOWN";
            case SYNTAX:
                return "SYNTAX";
            case SEMANTIC:
                return "SEMANTIC";
            case COMPILATION:
                return "COMPILATION";
            case EXECUTION:
                return "EXECUTION";
            default:
                return String.valueOf(code);
        }
    }
}
