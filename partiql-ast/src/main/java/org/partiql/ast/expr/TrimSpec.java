package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

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
    @Override
    public String name() {
        switch (code) {
            case LEADING: return "LEADING";
            case TRAILING: return "TRAILING";
            case BOTH: return "BOTH";
            default: return "UNKNOWN";
        }
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
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return null;
    }
}
