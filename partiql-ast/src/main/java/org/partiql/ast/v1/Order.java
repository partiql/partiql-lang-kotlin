package org.partiql.ast.v1;

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
}
