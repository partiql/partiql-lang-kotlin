package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.List;

/**
 * Represents SQL's truth value enum used for {@link ExprBoolTest}. E.g. {@code TRUE}, {@code FALSE}, {@code UNKNOWN}.
 */
@EqualsAndHashCode(callSuper = false)
public final class TruthValue extends AstEnum {
    /**
     * Bool test truth value for IS [NOT] TRUE.
     */
    public static final int TRUE = 0;
    /**
     * Bool test truth value for IS [NOT] FALSE.
     */
    public static final int FALSE = 1;
    /**
     * Bool test truth value for IS [NOT] UNKNOWN.
     */
    public static final int UNKNOWN = 2;

    private final int code;

    public TruthValue(int code) {
        this.code = code;
    }

    public static TruthValue TRUE() {
        return new TruthValue(TRUE);
    }
    
    public static TruthValue FALSE() {
        return new TruthValue(FALSE);
    }
    
    public static TruthValue UNKNOWN() {
        return new TruthValue(UNKNOWN);
    }

    @Override
    public int code() {
        return code;
    }

    @NotNull
    @Override
    public String name() {
        switch (code) {
            case TRUE: return "TRUE";
            case FALSE: return "FALSE";
            case UNKNOWN: return "UNKNOWN";
            default: throw new IllegalStateException("Invalid TruthValue code: " + code);
        }
    }

    @NotNull
    public static TruthValue parse(@NotNull String value) {
        switch (value) {
            case "TRUE": return TruthValue.TRUE();
            case "FALSE": return TruthValue.FALSE();
            case "UNK": return TruthValue.UNKNOWN();
            default: throw new IllegalArgumentException("No enum constant TruthValue." + value);
        }
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return List.of();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return null;
    }
}
