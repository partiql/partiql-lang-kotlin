package org.partiql.ast.v1.expr;

import lombok.EqualsAndHashCode;
import org.partiql.ast.v1.Enum;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class WindowFunction implements Enum {
    public static final int UNKNOWN = 0;
    public static final int LAG = 0;
    public static final int LEAD = 0;

    public static WindowFunction UNKNOWN() {
        return new WindowFunction(UNKNOWN);
    }

    public static WindowFunction LAG() {
        return new WindowFunction(LAG);
    }

    public static WindowFunction LEAD() {
        return new WindowFunction(LEAD);
    }

    private final int code;

    public WindowFunction(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }

    public static WindowFunction valueOf(String value) {
        switch (value) {
            case "LAG": return LAG();
            case "LEAD": return LEAD();
            default: return UNKNOWN();
        }
    }

    public static WindowFunction[] values() {
        return new WindowFunction[] {
            LAG(),
            LEAD()
        };
    }
}
