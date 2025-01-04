package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Determines the order in which null and missing values are sorted.
 */
@EqualsAndHashCode(callSuper = false)
public final class Nulls extends AstEnum {
    /**
     * Sort null and missing first.
     */
    public static final int FIRST = 0;
    /**
     * Sort null and missing last.
     */
    public static final int LAST = 1;

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
            default: throw new IllegalStateException("Invalid Nulls code: " + code);
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
            case "FIRST": return FIRST();
            case "LAST": return LAST();
            default: throw new IllegalArgumentException("No enum constant Nulls." + value);
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
