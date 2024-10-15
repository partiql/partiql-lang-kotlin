package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.PEnum;

/**
 * TODO
 */
public final class Severity extends PEnum {

    private Severity(int code) {
        super(code);
    }

    /**
     * TODO
     */
    public static final int UNKNOWN = 0;

    /**
     * TODO
     */
    public static final int ERROR = 1;

    /**
     * 
     * @return TODO
     */
    @NotNull
    public static Severity ERROR() {
        return new Severity(ERROR);
    }

    /**
     * TODO
     */
    public static final int WARNING = 2;

    /**
     *
     * @return TODO
     */
    @NotNull
    public static Severity WARNING() {
        return new Severity(WARNING);
    }

    /**
     * This is subject to change without prior notice.
     * @return a string representation of {@link Severity}, for debugging purposes only
     */
    @Override
    public String toString() {
        int code = code();
        switch (code) {
            case ERROR:
                return "ERROR";
            case WARNING:
                return "WARNING";
            default:
                return String.valueOf(code);
        }
    }
}
