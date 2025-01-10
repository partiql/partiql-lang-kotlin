package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.Enum;
import org.partiql.spi.UnsupportedCodeException;

/**
 * An encoding for PartiQL values.
 */
public class Encoding extends Enum {

    private Encoding(int code) {
        super(code);
    }

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
    public String name() throws UnsupportedCodeException {
        int code = code();
        if (code == ION) {
            return "ION";
        }
        throw new UnsupportedCodeException(code);
    }
}
