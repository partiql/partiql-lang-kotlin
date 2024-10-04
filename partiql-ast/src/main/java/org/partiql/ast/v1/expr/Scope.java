package org.partiql.ast.v1.expr;

import org.partiql.ast.v1.Enum;

/**
 * TODO docs, equals, hashcode
 */
public class Scope implements Enum {
    public static final int UNKNOWN = 0;
    public static final int DEFAULT = 1;
    public static final int LOCAL = 2;

    public static Scope UNKNOWN() {
        return new Scope(UNKNOWN);
    }

    public static Scope DEFAULT() {
        return new Scope(DEFAULT);
    }

    public static Scope LOCAL() {
        return new Scope(LOCAL);
    }

    private final int code;

    private Scope(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
