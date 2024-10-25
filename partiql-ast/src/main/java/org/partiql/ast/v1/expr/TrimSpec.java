package org.partiql.ast.v1.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstEnum;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.Collection;
import java.util.Collections;

@EqualsAndHashCode(callSuper = false)
public class TrimSpec extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int LEADING = 1;
    public static final int TRAILING = 2;
    public static final int BOTH = 3;

    public static TrimSpec UNKNOWN() {
        return new TrimSpec(UNKNOWN);
    }

    public static TrimSpec LEADING() {
        return new TrimSpec(LEADING);
    }

    public static TrimSpec TRAILING() {
        return new TrimSpec(TRAILING);
    }

    public static TrimSpec BOTH() {
        return new TrimSpec(BOTH);
    }

    private final int code;

    private TrimSpec(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }

    @NotNull
    private static final int[] codes = {
        LEADING,
        TRAILING,
        BOTH
    };

    @NotNull
    public static TrimSpec parse(@NotNull String value) {
        switch (value) {
            case "LEADING": return LEADING();
            case "TRAILING": return TRAILING();
            case "BOTH": return BOTH();
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
