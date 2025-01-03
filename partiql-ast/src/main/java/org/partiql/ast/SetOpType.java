package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents SQL set operations. E.g. {@code UNION}.
 */
@EqualsAndHashCode(callSuper = false)
public final class SetOpType extends AstEnum {
    /**
     * UNION set op variant.
     */
    public static final int UNION = 0;
    /**
     * INTERSECT set op variant.
     */
    public static final int INTERSECT = 1;
    /**
     * EXCEPT set op variant.
     */
    public static final int EXCEPT = 2;

    public static SetOpType UNION() {
        return new SetOpType(UNION);
    }

    public static SetOpType INTERSECT() {
        return new SetOpType(INTERSECT);
    }

    public static SetOpType EXCEPT() {
        return new SetOpType(EXCEPT);
    }

    private final int code;

    private SetOpType(int code) {
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
            case UNION: return "UNION";
            case INTERSECT: return "INTERSECT";
            case EXCEPT: return "EXCEPT";
            default: throw new IllegalStateException("Invalid SetOpType code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
        UNION,
        INTERSECT,
        EXCEPT
    };

    @NotNull
    public static SetOpType parse(@NotNull String value) {
        switch (value) {
            case "UNION": return UNION();
            case "INTERSECT": return INTERSECT();
            case "EXCEPT": return EXCEPT();
            default: throw new IllegalArgumentException("No enum constant SetOpType." + value);
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
