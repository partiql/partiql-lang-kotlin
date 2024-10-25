package org.partiql.ast.v1;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class FromType extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int SCAN = 1;
    public static final int UNPIVOT = 2;

    public static FromType UNKNOWN() {
        return new FromType(UNKNOWN);
    }

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
    private static final int[] codes = {
        SCAN,
        UNPIVOT
    };

    @NotNull
    public static FromType parse(@NotNull String value) {
        switch (value) {
            case "SCAN": return SCAN();
            case "UNPIVOT": return UNPIVOT();
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
