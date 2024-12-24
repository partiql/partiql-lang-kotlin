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
public class GraphRestrictor extends AstEnum {
    public static final int TRAIL = 0;
    public static final int ACYCLIC = 1;
    public static final int SIMPLE = 2;

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
            default: throw new IllegalStateException("Invalid GraphRestrictor code: " + code);
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
            default: throw new IllegalArgumentException("No enum constant GraphRestrictor." + value);
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
