package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * TODO
 */
@EqualsAndHashCode(callSuper = false)
public final class WindowFunctionSimpleName extends AstEnum {
    /**
     * TODO
     */
    public static final int RANK = 0;

    /**
     * TODO
     */
    public static final int DENSE_RANK = 1;

    /**
     * TODO
     */
    public static final int PERCENT_RANK = 2;

    /**
     * TODO
     */
    public static final int CUME_DIST = 3;

    /**
     * TODO
     */
    public static final int ROW_NUMBER = 4;

    /**
     * TODO
     * @return TODO
     */
    public static WindowFunctionSimpleName RANK() {
        return new WindowFunctionSimpleName(RANK);
    }

    /**
     * TODO
     * @return TODO
     */
    public static WindowFunctionSimpleName DENSE_RANK() {
        return new WindowFunctionSimpleName(DENSE_RANK);
    }

    /**
     * TODO
     * @return TODO
     */
    public static WindowFunctionSimpleName PERCENT_RANK() {
        return new WindowFunctionSimpleName(PERCENT_RANK);
    }

    /**
     * TODO
     * @return TODO
     */
    public static WindowFunctionSimpleName CUME_DIST() {
        return new WindowFunctionSimpleName(CUME_DIST);
    }

    /**
     * TODO
     * @return TODO
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
