package org.partiql.ast.graph;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class GraphDirection extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int LEFT = 1;
    public static final int UNDIRECTED = 2;
    public static final int RIGHT = 3;
    public static final int LEFT_OR_UNDIRECTED = 4;
    public static final int UNDIRECTED_OR_RIGHT = 5;
    public static final int LEFT_OR_RIGHT = 6;
    public static final int LEFT_UNDIRECTED_OR_RIGHT = 7;

    public static GraphDirection UNKNOWN() {
        return new GraphDirection(UNKNOWN);
    }

    public static GraphDirection LEFT() {
        return new GraphDirection(LEFT);
    }

    public static GraphDirection UNDIRECTED() {
        return new GraphDirection(UNDIRECTED);
    }

    public static GraphDirection RIGHT() {
        return new GraphDirection(RIGHT);
    }

    public static GraphDirection LEFT_OR_UNDIRECTED() {
        return new GraphDirection(LEFT_OR_UNDIRECTED);
    }

    public static GraphDirection UNDIRECTED_OR_RIGHT() {
        return new GraphDirection(UNDIRECTED_OR_RIGHT);
    }

    public static GraphDirection LEFT_OR_RIGHT() {
        return new GraphDirection(LEFT_OR_RIGHT);
    }

    public static GraphDirection LEFT_UNDIRECTED_OR_RIGHT() {
        return new GraphDirection(LEFT_UNDIRECTED_OR_RIGHT);
    }

    private final int code;

    @NotNull
    private static final int[] codes = {
        LEFT,
        UNDIRECTED,
        RIGHT,
        LEFT_OR_UNDIRECTED,
        UNDIRECTED_OR_RIGHT,
        LEFT_OR_RIGHT,
        LEFT_UNDIRECTED_OR_RIGHT
    };

    private GraphDirection(int code) {
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
            case LEFT: return "LEFT";
            case UNDIRECTED: return "UNDIRECTED";
            case RIGHT: return "RIGHT";
            case LEFT_OR_UNDIRECTED: return "LEFT_OR_UNDIRECTED";
            case UNDIRECTED_OR_RIGHT: return "UNDIRECTED_OR_RIGHT";
            case LEFT_OR_RIGHT: return "LEFT_OR_RIGHT";
            case LEFT_UNDIRECTED_OR_RIGHT: return "LEFT_UNDIRECTED_OR_RIGHT";
            default: return "UNKNOWN";
        }
    }

    @NotNull
    public static GraphDirection parse(@NotNull String value) {
        switch (value) {
            case "LEFT": return LEFT();
            case "UNDIRECTED": return UNDIRECTED();
            case "RIGHT": return RIGHT();
            case "LEFT_OR_UNDIRECTED": return LEFT_OR_UNDIRECTED();
            case "UNDIRECTED_OR_RIGHT": return UNDIRECTED_OR_RIGHT();
            case "LEFT_OR_RIGHT": return LEFT_OR_RIGHT();
            case "LEFT_UNDIRECTED_OR_RIGHT": return LEFT_UNDIRECTED_OR_RIGHT();
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
