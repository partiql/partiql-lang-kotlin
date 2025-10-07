package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Contains the set of no-arg window function names.
 * @see WindowFunctionType.NoArg#getName()
 */
@EqualsAndHashCode(callSuper = false)
public final class WindowFunctionSimpleName extends AstEnum {
    /**
     * The RANK function.
     */
    public static final int RANK = 0;

    /**
     * The DENSE_RANK function.
     */
    public static final int DENSE_RANK = 1;

    /**
     * The PERCENT_RANK function.
     */
    public static final int PERCENT_RANK = 2;

    /**
     * The CUME_DIST function.
     */
    public static final int CUME_DIST = 3;

    /**
     * The ROW_NUMBER function.
     */
    public static final int ROW_NUMBER = 4;

    /**
     * Constructs a new variant with the {@link #RANK} code.
     * @return a  new variant with the {@link #RANK} code
     */
    public static WindowFunctionSimpleName RANK() {
        return new WindowFunctionSimpleName(RANK);
    }

    /**
     * Constructs a new variant with the {@link #DENSE_RANK} code.
     * @return a new variant with the {@link #DENSE_RANK} code
     */
    public static WindowFunctionSimpleName DENSE_RANK() {
        return new WindowFunctionSimpleName(DENSE_RANK);
    }

    /**
     * Constructs a new variant with the {@link #PERCENT_RANK} code.
     * @return a new variant with the {@link #PERCENT_RANK} code
     */
    public static WindowFunctionSimpleName PERCENT_RANK() {
        return new WindowFunctionSimpleName(PERCENT_RANK);
    }

    /**
     * Constructs a new variant with the {@link #CUME_DIST} code.
     * @return a new variant with the {@link #CUME_DIST} code
     */
    public static WindowFunctionSimpleName CUME_DIST() {
        return new WindowFunctionSimpleName(CUME_DIST);
    }

    /**
     * Constructs a new variant with the {@link #ROW_NUMBER} code.
     * @return a new variant with the {@link #ROW_NUMBER} code
     */
    public static WindowFunctionSimpleName ROW_NUMBER() {
        return new WindowFunctionSimpleName(ROW_NUMBER);
    }

    private final int code;

    private WindowFunctionSimpleName(int code) {
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
            case RANK: return "RANK";
            case DENSE_RANK: return "DENSE_RANK";
            case PERCENT_RANK: return "PERCENT_RANK";
            case CUME_DIST: return "CUME_DIST";
            case ROW_NUMBER: return "ROW_NUMBER";
            default: throw new IllegalStateException("Invalid code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
        RANK,
        DENSE_RANK,
        PERCENT_RANK,
        CUME_DIST,
        ROW_NUMBER
    };

    /**
     * Returns the codes for the window function simple name.
     * @return the codes for the window function simple name
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
