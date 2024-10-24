package org.partiql.eval;

/**
 * PartiQL Execution Mode.
 */
public class Mode {

    /**
     * Strict execution mode.
     */
    public static final int STRICT = 0;

    /**
     * Permissive execution mode.
     */
    public static final int PERMISSIVE = 1;

    /**
     * Internal enum code.
     */
    private final int code;

    private Mode(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }

    public static Mode STRICT() {
        return new Mode(STRICT);
    }

    public static Mode PERMISSIVE() {
        return new Mode(PERMISSIVE);
    }
}
