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
    public static final int UNKNOWN = 0;
    public static final int TRUE = 1;
    public static final int FALSE = 2;
    public static final int UNK = 3;

    private final int code;

    public TruthValue(int code) {
        this.code = code;
    }

    public static TruthValue UNKNOWN() {
        return new TruthValue(UNKNOWN);
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
        return "";
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
