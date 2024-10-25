package org.partiql.ast.v1;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class SetQuantifier extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int ALL = 1;
    public static final int DISTINCT = 2;

    public static SetQuantifier UNKNOWN() {
        return new SetQuantifier(UNKNOWN);
    }

    public static SetQuantifier ALL() {
        return new SetQuantifier(ALL);
    }

    public static SetQuantifier DISTINCT() {
        return new SetQuantifier(DISTINCT);
    }

    private final int code;

    private SetQuantifier(int code) {
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
            case ALL: return "ALL";
            case DISTINCT: return "DISTINCT";
            default: return "UNKNOWN";
        }
    }

    @NotNull
    private static final int[] codes = {
        ALL,
        DISTINCT
    };

    @NotNull
    public static SetQuantifier parse(@NotNull String value) {
        switch (value) {
            case "ALL": return ALL();
            case "DISTINCT": return DISTINCT();
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
