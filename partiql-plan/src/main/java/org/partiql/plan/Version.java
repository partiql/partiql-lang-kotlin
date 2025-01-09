package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.Enum;

/**
 * A plan version.
 */
public class Version extends Enum {

    private Version(int code) {
        super(code);
    }

    @NotNull
    @Override
    public String name() {
        int code = code();
        switch (code) {
            case UNKNOWN:
                return "UNKNOWN";
            default:
                return String.valueOf(code);
        }
    }

    public static final int UNKNOWN = 0;

    public static Version UNKNOWN() {
        return new Version(UNKNOWN);
    }

    @Override
    public String toString() {
       int code = code();
       switch (code) {
           case UNKNOWN:
           default:
               return "UNKNOWN(" + code + ")";
       }
    }
}
