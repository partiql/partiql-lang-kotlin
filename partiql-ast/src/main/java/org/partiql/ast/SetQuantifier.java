package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class SetQuantifier extends AstEnum {
    public static final int ALL = 0;
    public static final int DISTINCT = 1;

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
            default: throw new IllegalStateException("Invalid SetQuantifier code: " + code);
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
            default: throw new IllegalArgumentException("No enum constant SetQuantifier." + value);
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
