package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class WindowFunction extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int LAG = 1;
    public static final int LEAD = 2;

    public static WindowFunction UNKNOWN() {
        return new WindowFunction(UNKNOWN);
    }

    public static WindowFunction LAG() {
        return new WindowFunction(LAG);
    }

    public static WindowFunction LEAD() {
        return new WindowFunction(LEAD);
    }

    private final int code;

    public WindowFunction(int code) {
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
            case LAG: return "LAG";
            case LEAD: return "LEAD";
            default: return "UNKNOWN";
        }
    }

    @NotNull
    private static final int[] codes = {
        LAG,
        LEAD
    };

    @NotNull
    public static WindowFunction parse(@NotNull String value) {
        switch (value) {
            case "LAG": return LAG();
            case "LEAD": return LEAD();
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
