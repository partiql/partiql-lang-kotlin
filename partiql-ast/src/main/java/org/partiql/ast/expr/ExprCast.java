package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.DataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a CAST expression. E.g. {@code CAST(col1 AS INTEGER)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprCast extends Expr {
    @NotNull
    private final Expr value;

    @NotNull
    private final DataType asType;

    public ExprCast(@NotNull Expr value, @NotNull DataType asType) {
        this.value = value;
        this.asType = asType;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        kids.add(asType);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprCast(this, ctx);
    }

    @NotNull
    public Expr getValue() {
        return this.value;
    }

    @NotNull
    public DataType getAsType() {
        return this.asType;
    }
}
