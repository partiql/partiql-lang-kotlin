package org.partiql.ast.v1;

/**
 * TODO docs, equals, hashcode
 */
public class SetQuantifier implements Enum {
    public static final int UNKNOWN = 0;
    public static final int ALL = 1;
    public static final int DISTINCT = 2;

    public static SetQuantifier UNKNOWN() {
        return new SetQuantifier(UNKNOWN);
    }

    public static SetQuantifier ALL() {
        return new SetQuantifier(ALL);
    }

    public static SetQuantifier DISTINCT() {
        return new SetQuantifier(DISTINCT);
    }

    private final int code;

    private SetQuantifier(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}