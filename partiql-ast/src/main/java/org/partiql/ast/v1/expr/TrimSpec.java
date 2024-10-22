package org.partiql.ast.v1.expr;

import lombok.EqualsAndHashCode;
import org.partiql.ast.v1.Enum;

@EqualsAndHashCode(callSuper = false)
public class TrimSpec implements Enum {
    public static final int UNKNOWN = 0;
    public static final int LEADING = 1;
    public static final int TRAILING = 2;
    public static final int BOTH = 3;

    public static TrimSpec UNKNOWN() {
        return new TrimSpec(UNKNOWN);
    }

    public static TrimSpec LEADING() {
        return new TrimSpec(LEADING);
    }

    public static TrimSpec TRAILING() {
        return new TrimSpec(TRAILING);
    }

    public static TrimSpec BOTH() {
        return new TrimSpec(BOTH);
    }

    private final int code;

    private TrimSpec(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }

    public static TrimSpec valueOf(String value) {
        switch (value) {
            case "LEADING": return LEADING();
            case "TRAILING": return TRAILING();
            case "BOTH": return BOTH();
            default: return UNKNOWN();
        }
    }

    public static TrimSpec[] values() {
        return new TrimSpec[] {
            LEADING(),
            TRAILING(),
            BOTH()
        };
    }
}
