package org.partiql.ast.v1;

/**
 * TODO docs, equals, hashcode
 */
public class SetOpType implements Enum {
    private static final int UNKNOWN = 0;
    private static final int UNION = 1;
    private static final int INTERSECT = 2;
    private static final int EXCEPT = 3;

    public static SetOpType UNKNOWN() {
        return new SetOpType(UNKNOWN);
    }

    public static SetOpType UNION() {
        return new SetOpType(UNION);
    }

    public static SetOpType INTERSECT() {
        return new SetOpType(INTERSECT);
    }

    public static SetOpType EXCEPT() {
        return new SetOpType(EXCEPT);
    }

    private final int code;

    private SetOpType(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
