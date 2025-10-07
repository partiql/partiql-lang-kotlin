package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * The null treatment for window functions.
 * @see WindowFunctionType.LeadOrLag#getNullTreatment()
 */
@EqualsAndHashCode(callSuper = false)
public final class WindowFunctionNullTreatment extends AstEnum {
    /**
     * The RESPECT NULLS variant of the null treatment.
     */
    public static final int RESPECT_NULLS = 0;

    /**
     * The IGNORE NULLS variant of the null treatment.
     */
    public static final int IGNORE_NULLS = 1;

    /**
     * Constructs a new window function null treatment with the {@link #RESPECT_NULLS} code.
     * @return a new window function null treatment with the {@link #RESPECT_NULLS} code
     */
    public static WindowFunctionNullTreatment RESPECT_NULLS() {
        return new WindowFunctionNullTreatment(RESPECT_NULLS);
    }

    /**
     * Constructs a new window function null treatment with the {@link #IGNORE_NULLS} code.
     * @return a  new window function null treatment with the {@link #IGNORE_NULLS} code
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

    /**
     * Returns the codes for the window function null treatment.
     * @return the codes for the window function null treatment
     */
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
