package org.partiql.ast.v1;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

@EqualsAndHashCode(callSuper = false)
public class Order extends AstEnum {
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

    @NotNull
    @Override
    public String name() {
        switch (code) {
            case ASC: return "ASC";
            case DESC: return "DESC";
            default: return "UNKNOWN";
        }
    }

    @NotNull
    private static final int[] codes = {
        ASC,
        DESC
    };

    @NotNull
    public static Order parse(@NotNull String value) {
        switch (value) {
            case "ASC": return ASC();
            case "DESC": return DESC();
            default: return UNKNOWN();
        }
    }

    @NotNull
    public static int[] codes() {
        return codes;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return null;
    }
}
