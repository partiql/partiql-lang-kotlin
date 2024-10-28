package org.partiql.ast.v1.graph;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstEnum;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class GraphRestrictor extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int TRAIL = 1;
    public static final int ACYCLIC = 2;
    public static final int SIMPLE = 3;

    public static GraphRestrictor UNKNOWN() {
        return new GraphRestrictor(UNKNOWN);
    }

    public static GraphRestrictor TRAIL() {
        return new GraphRestrictor(TRAIL);
    }

    public static GraphRestrictor ACYCLIC() {
        return new GraphRestrictor(ACYCLIC);
    }

    public static GraphRestrictor SIMPLE() {
        return new GraphRestrictor(SIMPLE);
    }

    private final int code;

    private GraphRestrictor(int code) {
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
            case TRAIL: return "TRAIL";
            case ACYCLIC: return "ACYCLIC";
            case SIMPLE: return "SIMPLE";
            default: return "UNKNOWN";
        }
    }

    @NotNull
    private static final int[] codes = {
        TRAIL,
        ACYCLIC,
        SIMPLE
    };

    @NotNull
    public static GraphRestrictor parse(@NotNull String value) {
        switch (value) {
            case "TRAIL": return TRAIL();
            case "ACYCLIC": return ACYCLIC();
            case "SIMPLE": return SIMPLE();
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
