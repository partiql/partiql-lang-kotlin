package org.partiql.ast.v1;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class Order implements Enum {
    public static final int UNKNOWN = 0;
    public static final int ASC = 1;
    public static final int DESC = 2;

    public static Order UNKNOWN() {
        return new Order(UNKNOWN);
    }

    public static Order ASC() {
        return new Order(ASC);
    }

    public static Order DESC() {
        return new Order(DESC);
    }

    private final int code;

    private Order(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }

    public static Order valueOf(String value) {
        switch (value) {
            case "ASC": return ASC();
            case "DESC": return DESC();
            default: return UNKNOWN();
        }
    }

    public static Order[] values() {
        return new Order[] {
            ASC(),
            DESC()
        };
    }
}
