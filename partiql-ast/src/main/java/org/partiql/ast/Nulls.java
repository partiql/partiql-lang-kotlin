package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class Nulls extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int FIRST = 1;
    public static final int LAST = 2;

    public static Nulls UNKNOWN() {
        return new Nulls(UNKNOWN);
    }

    public static Nulls FIRST() {
        return new Nulls(FIRST);
    }

    public static Nulls LAST() {
        return new Nulls(LAST);
    }

    private final int code;

    private Nulls(int code) {
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
            case FIRST: return "FIRST";
            case LAST: return "LAST";
            default: return "UNKNOWN";
        }
    }

    @NotNull
    private static final int[] codes = {
        FIRST,
        LAST
    };

    @NotNull
    public static Nulls parse(@NotNull String value) {
        switch (value) {
            case "UNKNOWN": return UNKNOWN();
            case "FIRST": return FIRST();
            case "LAST": return LAST();
            default: return UNKNOWN();
        }
    }

    @NotNull
    public static int[] codes() {
        return codes;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return null;
    }
}
