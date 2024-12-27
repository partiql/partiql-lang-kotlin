package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public final class Scope extends AstEnum {
    public static final int DEFAULT = 0;
    public static final int LOCAL = 1;

    public static Scope DEFAULT() {
        return new Scope(DEFAULT);
    }

    public static Scope LOCAL() {
        return new Scope(LOCAL);
    }

    private final int code;

    private Scope(int code) {
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
            case DEFAULT: return "DEFAULT";
            case LOCAL: return "LOCAL";
            default: throw new IllegalStateException("Invalid Scope code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
        DEFAULT,
        LOCAL
    };

    @NotNull
    public static Scope parse(@NotNull String value) {
        switch (value) {
            case "DEFAULT": return DEFAULT();
            case "LOCAL": return LOCAL();
            default: throw new IllegalArgumentException("No enum constant Scope." + value);
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
