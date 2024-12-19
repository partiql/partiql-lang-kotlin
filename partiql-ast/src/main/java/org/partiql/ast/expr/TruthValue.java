package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.List;

/**
 * TODO docs
 */
@EqualsAndHashCode(callSuper = false)
public class TruthValue extends AstEnum {
    public static final int TRUE = 0;
    public static final int FALSE = 1;
    public static final int UNK = 2;

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
    
    public static TruthValue UNK() {
        return new TruthValue(UNK);
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
            case UNK: return "UNK";
            default: throw new IllegalStateException("Invalid TruthValue code: " + code);
        }
    }

    @NotNull
    public static TruthValue parse(@NotNull String value) {
        switch (value) {
            case "TRUE": return TruthValue.TRUE();
            case "FALSE": return TruthValue.FALSE();
            case "UNK": return TruthValue.UNK();
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
