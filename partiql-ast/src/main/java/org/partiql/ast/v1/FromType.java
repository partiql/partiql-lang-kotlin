package org.partiql.ast.v1;

/**
 * TODO docs, equals, hashcode
 */
public class FromType implements Enum {
    public static final int UNKNOWN = 0;
    public static final int SCAN = 1;
    public static final int UNPIVOT = 2;

    public static FromType UNKNOWN() {
        return new FromType(UNKNOWN);
    }

    public static FromType SCAN() {
        return new FromType(SCAN);
    }

    public static FromType UNPIVOT() {
        return new FromType(UNPIVOT);
    }

    private final int code;

    private FromType(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
