package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class GroupByStrategy extends AstEnum {
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
            default: return "UNKNOWN";
        }
    }

    @NotNull
    public static GroupByStrategy parse(@NotNull String value) {
        switch (value) {
            case "FULL": return FULL();
            case "PARTIAL": return PARTIAL();
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
