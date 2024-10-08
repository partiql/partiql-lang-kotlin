package org.partiql.ast.v1.expr;

import org.partiql.ast.v1.Enum;

/**
 * TODO docs, equals, hashcode
 */
public class SessionAttribute implements Enum {
    public static final int UNKNOWN = 0;
    public static final int CURRENT_USER = 1;
    public static final int CURRENT_DATE = 2;

    public static SessionAttribute UNKNOWN() {
        return new SessionAttribute(UNKNOWN);
    }

    public static SessionAttribute CURRENT_USER() {
        return new SessionAttribute(CURRENT_USER);
    }

    public static SessionAttribute CURRENT_DATE() {
        return new SessionAttribute(CURRENT_DATE);
    }

    private final int code;

    public SessionAttribute(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
