package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class Scope extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int DEFAULT = 1;
    public static final int LOCAL = 2;

    public static Scope UNKNOWN() {
        return new Scope(UNKNOWN);
    }

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
            default: return "UNKNOWN";
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
            case "UNKNOWN": return UNKNOWN();
            case "DEFAULT": return DEFAULT();
            case "LOCAL": return LOCAL();
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