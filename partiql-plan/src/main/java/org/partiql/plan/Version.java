package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.Enum;
import org.partiql.spi.UnsupportedCodeException;

/**
 * A plan version.
 */
public class Version extends Enum {

    private Version(int code) {
        super(code);
    }

    @NotNull
    @Override
    public String name() throws UnsupportedCodeException {
        int code = code();
        if (code == V1) {
            return "V1";
        }
        throw new UnsupportedCodeException(code);
    }

    public static final int V1 = 0;

    public static Version V1() {
        return new Version(V1);
    }

    @Override
    public String toString() {
        try {
            return name();
        } catch (UnsupportedCodeException e) {
            return "UNRECOGNIZED_VERSION_CODE(" + code() + ")";
        }
    }
}
