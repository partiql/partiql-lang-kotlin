package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
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
    public List<AstNode> getChildren() {
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
