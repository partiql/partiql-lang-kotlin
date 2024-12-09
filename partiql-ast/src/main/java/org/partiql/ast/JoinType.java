package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class JoinType extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int INNER = 1;
    public static final int LEFT = 2;
    public static final int LEFT_OUTER = 3;
    public static final int RIGHT = 4;
    public static final int RIGHT_OUTER = 5;
    public static final int FULL = 6;
    public static final int FULL_OUTER = 7;
    public static final int CROSS = 8;
    public static final int LEFT_CROSS = 9;

    public static JoinType UNKNOWN() {
        return new JoinType(UNKNOWN);
    }

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
            default: return "UNKNOWN";
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
