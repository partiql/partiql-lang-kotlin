package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents the support join types in PartiQL.
 */
@EqualsAndHashCode(callSuper = false)
public final class JoinType extends AstEnum {
    /**
     * Inner join variant.
     */
    public static final int INNER = 0;
    /**
     * Left join variant.
     */
    public static final int LEFT = 1;
    /**
     * Left outer join variant.
     */
    public static final int LEFT_OUTER = 2;
    /**
     * Right join variant.
     */
    public static final int RIGHT = 3;
    /**
     * Right outer join variant.
     */
    public static final int RIGHT_OUTER = 4;
    /**
     * Full join variant.
     */
    public static final int FULL = 5;
    /**
     * Full outer join variant.
     */
    public static final int FULL_OUTER = 6;
    /**
     * Cross join variant.
     */
    public static final int CROSS = 7;
    /**
     * Left cross join variant.
     */
    public static final int LEFT_CROSS = 8;

    public static JoinType INNER() {
        return new JoinType(INNER);
    }

    public static JoinType LEFT() {
        return new JoinType(LEFT);
    }

    public static JoinType LEFT_OUTER() {
        return new JoinType(LEFT_OUTER);
    }

    public static JoinType RIGHT() {
        return new JoinType(RIGHT);
    }

    public static JoinType RIGHT_OUTER() {
        return new JoinType(RIGHT_OUTER);
    }

    public static JoinType FULL() {
        return new JoinType(FULL);
    }

    public static JoinType FULL_OUTER() {
        return new JoinType(FULL_OUTER);
    }

    public static JoinType CROSS() {
        return new JoinType(CROSS);
    }

    public static JoinType LEFT_CROSS() {
        return new JoinType(LEFT_CROSS);
    }

    private final int code;

    private JoinType(int code) {
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
            case INNER: return "INNER";
            case LEFT: return "LEFT";
            case LEFT_OUTER: return "LEFT_OUTER";
            case RIGHT: return "RIGHT";
            case RIGHT_OUTER: return "RIGHT_OUTER";
            case FULL: return "FULL";
            case FULL_OUTER: return "FULL_OUTER";
            case CROSS: return "CROSS";
            case LEFT_CROSS: return "LEFT_CROSS";
            default: throw new IllegalStateException("Invalid JoinType code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
        INNER,
        LEFT,
        LEFT_OUTER,
        RIGHT,
        RIGHT_OUTER,
        FULL,
        FULL_OUTER,
        CROSS,
        LEFT_CROSS
    };

    @NotNull
    public static JoinType parse(@NotNull String value) {
        switch (value) {
            case "INNER": return INNER();
            case "LEFT": return LEFT();
            case "LEFT_OUTER": return LEFT_OUTER();
            case "RIGHT": return RIGHT();
            case "RIGHT_OUTER": return RIGHT_OUTER();
            case "FULL": return FULL();
            case "FULL_OUTER": return FULL_OUTER();
            case "CROSS": return CROSS();
            case "LEFT_CROSS": return LEFT_CROSS();
            default: throw new IllegalArgumentException("No enum constant JoinType." + value);
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
