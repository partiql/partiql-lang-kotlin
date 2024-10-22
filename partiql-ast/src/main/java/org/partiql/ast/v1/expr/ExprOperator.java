package org.partiql.ast.v1.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprOperator extends Expr {
    @NotNull
    public final String symbol;

    @Nullable
    public final Expr lhs;

    @NotNull
    public final Expr rhs;

    public ExprOperator(@NotNull String symbol, @Nullable Expr lhs, @NotNull Expr rhs) {
        this.symbol = symbol;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        if (lhs != null) {
            kids.add(lhs);
        }
        kids.add(rhs);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprOperator(this, ctx);
    }
}
