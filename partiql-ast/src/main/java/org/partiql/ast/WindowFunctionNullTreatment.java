package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * TODO
 */
@EqualsAndHashCode(callSuper = false)
public final class WindowFunctionNullTreatment extends AstEnum {
    /**
     * TODO
     */
    public static final int RESPECT_NULLS = 0;

    /**
     * TODO
     */
    public static final int IGNORE_NULLS = 1;

    /**
     * TODO
     * @return TODO
     */
    public static WindowFunctionNullTreatment RESPECT_NULLS() {
        return new WindowFunctionNullTreatment(RESPECT_NULLS);
    }

    /**
     * TODO
     * @return TODO
     */
    public static WindowFunctionNullTreatment IGNORE_NULLS() {
        return new WindowFunctionNullTreatment(IGNORE_NULLS);
    }

    private final int code;

    private WindowFunctionNullTreatment(int code) {
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
            case RESPECT_NULLS: return "RESPECT_NULLS";
            case IGNORE_NULLS: return "DISTINCT";
            default: throw new IllegalStateException("Invalid code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
            RESPECT_NULLS,
            IGNORE_NULLS
    };

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
