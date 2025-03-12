package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Represents a SQL window function enum. E.g. {@code LAG}.
 * @deprecated This is replaced by {@link org.partiql.ast.expr.ExprWindowFunction}.
 */
@EqualsAndHashCode(callSuper = false)
@Deprecated
public final class WindowFunction extends AstEnum {
    /**
     * LAG window function.
     */
    public static final int LAG = 0;
    /**
     * LEAD window function.
     */
    public static final int LEAD = 1;

    public static WindowFunction LAG() {
        return new WindowFunction(LAG);
    }

    public static WindowFunction LEAD() {
        return new WindowFunction(LEAD);
    }

    private final int code;

    public WindowFunction(int code) {
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
            case LAG: return "LAG";
            case LEAD: return "LEAD";
            default: throw new IllegalStateException("Invalid WindowFunction code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
        LAG,
        LEAD
    };

    @NotNull
    public static WindowFunction parse(@NotNull String value) {
        switch (value) {
            case "LAG": return LAG();
            case "LEAD": return LEAD();
            default: throw new IllegalArgumentException("No enum constant WindowFunction." + value);
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
