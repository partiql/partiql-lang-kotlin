package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single FROM source.
 */
@EqualsAndHashCode(callSuper = false)
public final class FromType extends AstEnum {
    /**
     * A table scan.
     */
    public static final int SCAN = 0;
    /**
     * A table unpivot.
     */
    public static final int UNPIVOT = 1;

    public static FromType SCAN() {
        return new FromType(SCAN);
    }

    public static FromType UNPIVOT() {
        return new FromType(UNPIVOT);
    }

    private final int code;

    private FromType(int code) {
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
            case SCAN: return "SCAN";
            case UNPIVOT: return "UNPIVOT";
            default: throw new IllegalStateException("Invalid FromType code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
        SCAN,
        UNPIVOT
    };

    @NotNull
    public static FromType parse(@NotNull String value) {
        switch (value) {
            case "SCAN": return SCAN();
            case "UNPIVOT": return UNPIVOT();
            default: throw new IllegalArgumentException("No enum constant FromType." + value);
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
