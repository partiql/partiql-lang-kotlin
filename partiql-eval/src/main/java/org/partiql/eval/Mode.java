package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.Enum;
import org.partiql.spi.UnsupportedCodeException;

/**
 * PartiQL Execution Mode.
 */
public class Mode extends Enum {

    /**
     * Strict execution mode.
     */
    public static final int STRICT = 0;

    /**
     * Permissive execution mode.
     */
    public static final int PERMISSIVE = 1;

    private Mode(int code) {
        super(code);
    }

    @NotNull
    @Override
    public String name() throws UnsupportedCodeException {
        int code = code();
        switch (code) {
            case STRICT:
                return "STRICT";
            case PERMISSIVE:
                return "PERMISSIVE";
            default:
                throw new UnsupportedCodeException(code);
        }
    }

    /**
     * Returns the {@link Mode#STRICT} execution mode.
     * @return the {@link Mode#STRICT} execution mode
     */
    public static Mode STRICT() {
        return new Mode(STRICT);
    }

    /**
     * Returns the {@link Mode#PERMISSIVE} execution mode.
     * @return the {@link Mode#PERMISSIVE} execution mode
     */
    public static Mode PERMISSIVE() {
        return new Mode(PERMISSIVE);
    }
}
