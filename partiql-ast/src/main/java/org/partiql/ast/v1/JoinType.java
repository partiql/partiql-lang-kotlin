package org.partiql.ast.v1;

/**
 * TODO docs, equals, hashcode
 */
public class JoinType implements Enum {
    public static final int UNKNOWN = 0;
    public static final int INNER = 1;
    public static final int LEFT = 2;
    public static final int LEFT_OUTER = 3;
    public static final int RIGHT = 4;
    public static final int RIGHT_OUTER = 5;
    public static final int FULL = 6;
    public static final int FULL_OUTER = 7;
    public static final int CROSS = 8;

    public static JoinType UNKNOWN() {
        return new JoinType(UNKNOWN);
    }

    public static JoinType INNER() {
        return new JoinType(INNER);
    }

    public static JoinType LEFT() {
        return new JoinType(LEFT);
    }

    public static JoinType LEFT_OUTER() {
        return new JoinType(LEFT_OUTER);
    }

    public static JoinType RIGHT() {
        return new JoinType(RIGHT);
    }

    public static JoinType RIGHT_OUTER() {
        return new JoinType(RIGHT_OUTER);
    }

    public static JoinType FULL() {
        return new JoinType(FULL);
    }

    public static JoinType FULL_OUTER() {
        return new JoinType(FULL_OUTER);
    }

    public static JoinType CROSS() {
        return new JoinType(CROSS);
    }

    private final int code;

    private JoinType(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
