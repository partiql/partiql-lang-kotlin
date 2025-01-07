package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Represents the TRIM specification used by {@link ExprTrim}. E.g. {@code LEADING}.
 */
@EqualsAndHashCode(callSuper = false)
public final class TrimSpec extends AstEnum {
    /**
     * TRIM specification applied to leading characters.
     */
    public static final int LEADING = 0;
    /**
     * TRIM specification applied to trailing characters.
     */
    public static final int TRAILING = 1;
    /**
     * TRIM specification applied to both leading and trailing characters.
     */
    public static final int BOTH = 2;

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
            default: throw new IllegalStateException("Invalid TrimSpec code: " + code);
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
            default: throw new IllegalArgumentException("No enum constant TrimSpec." + value);
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
