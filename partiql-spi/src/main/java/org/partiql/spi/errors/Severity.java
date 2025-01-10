package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.Enum;
import org.partiql.spi.UnsupportedCodeException;

/**
 * Identifies the "severity" of the associated {@link PError}. All variants are represented as static final integers
 * in this class.
 * @see PError#severity
 */
public final class Severity extends Enum {

    private Severity(int code) {
        super(code);
    }

    @NotNull
    @Override
    public String name() throws UnsupportedCodeException {
        int code = code();
        switch (code) {
            case ERROR:
                return "ERROR";
            case WARNING:
                return "WARNING";
            default:
                throw new UnsupportedCodeException(code);
        }
    }

    /**
     * Represents an error that should <b>not</b> proceed with processing further than the current component.
     */
    public static final int ERROR = 1;

    /**
     * 
     * @return a {@link Severity} with type {@link Severity#ERROR}.
     */
    @NotNull
    public static Severity ERROR() {
        return new Severity(ERROR);
    }

    /**
     * Represents a warning that <b>may</b> proceed with processing further than the current component.
     */
    public static final int WARNING = 2;

    /**
     * @return a {@link Severity} with type {@link Severity#WARNING}.
     */
    @NotNull
    public static Severity WARNING() {
        return new Severity(WARNING);
    }
}
