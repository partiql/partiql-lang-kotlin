package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.Enum;
import org.partiql.spi.UnsupportedCodeException;

/**
 * PartiQL JOIN types.
 */
public class JoinType extends Enum {

    public static final int UNKNOWN = 0;
    public static final int INNER = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final int FULL = 4;

    @NotNull
    @Override
    public String name() throws UnsupportedCodeException {
        int code = code();
        switch (code) {
            case INNER:
                return "INNER";
            case LEFT:
                return "LEFT";
            case RIGHT:
                return "RIGHT";
            case FULL:
                return "FULL";
            default:
                throw new UnsupportedCodeException(code);
        }
    }

    private JoinType(int value) {
        super(value);
    }

    @NotNull
    public static JoinType INNER() {
        return new JoinType(INNER);
    }

    @NotNull
    public static JoinType LEFT() {
        return new JoinType(LEFT);
    }

    @NotNull
    public static JoinType RIGHT() {
        return new JoinType(RIGHT);
    }

    @NotNull
    public static JoinType FULL() {
        return new JoinType(FULL);
    }
}
