package org.partiql.ast.v1;

import lombok.EqualsAndHashCode;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class GroupByStrategy implements Enum {
    public static final int UNKNOWN = 0;
    public static final int FULL = 1;
    public static final int PARTIAL = 2;

    public static GroupByStrategy UNKNOWN() {
        return new GroupByStrategy(UNKNOWN);
    }

    public static GroupByStrategy FULL() {
        return new GroupByStrategy(FULL);
    }

    public static GroupByStrategy PARTIAL() {
        return new GroupByStrategy(PARTIAL);
    }

    private final int code;

    private GroupByStrategy(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
