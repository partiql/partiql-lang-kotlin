package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.Enum;

/**
 * An encoding for PartiQL values.
 */
public class Encoding extends Enum {

    private Encoding(int code) {
        super(code);
    }

    /**
     * This variant is meant for forward-compatibility and should not be used to represent an {@link Encoding}.
     */
    public static final int UNKNOWN = 0;

    /**
     * Represents the canonical Ion encoding of PartiQL values.
     */
    public static final int ION = 1;

    /**
     * @return a {@link Encoding} with code {@link Encoding#ION}
     */
    @NotNull
    public static Encoding ION() {
        return new Encoding(ION);
    }

    @NotNull
    @Override
    public String name() {
        return toString();
    }

    @Override
    public String toString() {
        int code = code();
        switch (code) {
            case ION:
                return "ION";
            case UNKNOWN:
                return "UNKNOWN";
            default:
                return String.valueOf(code);
        }
    }
}
