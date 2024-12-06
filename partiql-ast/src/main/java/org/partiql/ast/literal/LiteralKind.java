package org.partiql.ast.literal;

import lombok.EqualsAndHashCode;

// TODO convert to an enum
@EqualsAndHashCode(callSuper = false)
public class LiteralKind {
    public static final int UNKNOWN = 0;
    public static final int NULL = 1;
    public static final int MISSING = 2;
    public static final int BOOLEAN = 3;
    public static final int NUM_APPROX = 4;
    public static final int NUM_EXACT = 5;
    public static final int NUM_INT = 6;
    public static final int STRING = 7;
    public static final int TYPED_STRING = 8;

    public static LiteralKind UNKNOWN() {
        return new LiteralKind(UNKNOWN);
    }

    public static LiteralKind NULL() {
        return new LiteralKind(NULL);
    }

    public static LiteralKind MISSING() {
        return new LiteralKind(MISSING);
    }

    public static LiteralKind BOOLEAN() {
        return new LiteralKind(BOOLEAN);
    }

    public static LiteralKind NUM_APPROX() {
        return new LiteralKind(NUM_APPROX);
    }

    public static LiteralKind NUM_EXACT() {
        return new LiteralKind(NUM_EXACT);
    }

    public static LiteralKind NUM_INT() {
        return new LiteralKind(NUM_INT);
    }

    public static LiteralKind STRING() {
        return new LiteralKind(STRING);
    }

    public static LiteralKind TYPED_STRING() {
        return new LiteralKind(TYPED_STRING);
    }

    private final int code;

    private LiteralKind(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
