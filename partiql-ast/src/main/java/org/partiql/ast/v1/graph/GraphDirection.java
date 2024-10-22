package org.partiql.ast.v1.graph;

import lombok.EqualsAndHashCode;
import org.partiql.ast.v1.Enum;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class GraphDirection implements Enum {
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

    private GraphDirection(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }

    public static GraphDirection valueOf(String value) {
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

    public static GraphDirection[] values() {
        return new GraphDirection[] {
            LEFT(),
            UNDIRECTED(),
            RIGHT(),
            LEFT_OR_UNDIRECTED(),
            UNDIRECTED_OR_RIGHT(),
            LEFT_OR_RIGHT(),
            LEFT_UNDIRECTED_OR_RIGHT()
        };
    }
}
