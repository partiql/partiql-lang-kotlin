package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.PEnum;

/**
 * Identifies the "type" of a {@link PError}, which can help consumers distinguish where the {@link PError} is coming
 * from. All variants are represented as static final integers in this class.
 * @see PError#kind
 */
public final class PErrorKind extends PEnum {

    private PErrorKind(int code) {
        super(code);
    }

    /**
     * This variant is meant for forward-compatibility and should not be used to represent a {@link PErrorKind}.
     */
    public static final int UNKNOWN = 0;

    /**
     * @return a {@link PErrorKind} of type {@link PErrorKind#UNKNOWN}.
     */
    @NotNull
    public static PErrorKind UNKNOWN() {
        return new PErrorKind(UNKNOWN);
    }

    /**
     * Represents a syntactical error (lexing/parsing).
     */
    public static final int SYNTAX = 1;

    /**
     * @return a {@link PErrorKind} of type {@link PErrorKind#SYNTAX}.
     */
    @NotNull
    public static PErrorKind SYNTAX() {
        return new PErrorKind(SYNTAX);
    }

    /**
     * TODO
     */
    public static final int SEMANTIC = 2;

    /**
     * @return a {@link PErrorKind} of type {@link PErrorKind#SEMANTIC}.
     */
    @NotNull
    public static PErrorKind SEMANTIC() {
        return new PErrorKind(SEMANTIC);
    }

    /**
     * Represents an error that has occurred while compiling a plan into an executable.
     */
    public static final int COMPILATION = 3;

    /**
     * @return a {@link PErrorKind} of type {@link PErrorKind#COMPILATION}.
     */
    @NotNull
    public static PErrorKind COMPILATION() {
        return new PErrorKind(COMPILATION);
    }

    /**
     * Represents an error that has occurred while executing an executable.
     */
    public static final int EXECUTION = 4;

    /**
     * @return a {@link PErrorKind} of type {@link PErrorKind#EXECUTION}.
     */
    @NotNull
    public static PErrorKind EXECUTION() {
        return new PErrorKind(EXECUTION);
    }

    /**
     * This is subject to change without prior notice.
     * @return a string representation of {@link PErrorKind}, for debugging purposes only
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
