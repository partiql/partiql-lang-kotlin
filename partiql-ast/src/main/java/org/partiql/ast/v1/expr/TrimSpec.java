package org.partiql.ast.v1.expr;

import org.partiql.ast.v1.Enum;

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
}
