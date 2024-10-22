package org.partiql.ast.v1;

import lombok.EqualsAndHashCode;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class Nulls implements Enum {
    public static final int UNKNOWN = 0;
    public static final int FIRST = 1;
    public static final int LAST = 2;

    public static Nulls UNKNOWN() {
        return new Nulls(UNKNOWN);
    }

    public static Nulls FIRST() {
        return new Nulls(FIRST);
    }

    public static Nulls LAST() {
        return new Nulls(LAST);
    }

    private final int code;

    private Nulls(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }

    public static Nulls valueOf(String value) {
        switch (value) {
            case "UNKNOWN": return UNKNOWN();
            case "FIRST": return FIRST();
            case "LAST": return LAST();
            default: return UNKNOWN();
        }
    }

    public static Nulls[] values() {
        return new Nulls[] {
            FIRST(),
            LAST()
        };
    }
}
