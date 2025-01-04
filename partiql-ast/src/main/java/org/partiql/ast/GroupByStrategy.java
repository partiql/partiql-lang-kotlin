package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents SQL's GROUP BY strategy.
 */
@EqualsAndHashCode(callSuper = false)
public final class GroupByStrategy extends AstEnum {
    /**
     * Group by FULL strategy. Commonly the default if unspecified.
     */
    public static final int FULL = 0;
    /**
     * Group by PARTIAL strategy.
     */
    public static final int PARTIAL = 1;

    public static GroupByStrategy FULL() {
        return new GroupByStrategy(FULL);
    }

    public static GroupByStrategy PARTIAL() {
        return new GroupByStrategy(PARTIAL);
    }

    private final int code;

    @NotNull
    private static final int[] codes = {
        FULL,
        PARTIAL
    };

    private GroupByStrategy(int code) {
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
            case FULL: return "FULL";
            case PARTIAL: return "PARTIAL";
            default: throw new IllegalStateException("Invalid GroupByStrategy code: " + code);
        }
    }

    @NotNull
    public static GroupByStrategy parse(@NotNull String value) {
        switch (value) {
            case "FULL": return FULL();
            case "PARTIAL": return PARTIAL();
            default: throw new IllegalArgumentException("No enum constant GroupByStrategy." + value);
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
