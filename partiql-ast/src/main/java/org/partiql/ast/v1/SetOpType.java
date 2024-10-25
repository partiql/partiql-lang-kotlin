package org.partiql.ast.v1;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class SetOpType extends AstEnum {
    private static final int UNKNOWN = 0;
    private static final int UNION = 1;
    private static final int INTERSECT = 2;
    private static final int EXCEPT = 3;

    public static SetOpType UNKNOWN() {
        return new SetOpType(UNKNOWN);
    }

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
