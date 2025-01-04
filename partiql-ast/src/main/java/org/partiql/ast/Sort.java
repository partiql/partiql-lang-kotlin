package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Sort specification. &lt;expr&gt; [ASC|DESC] [NULLS FIRST | NULLS LAST]
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Sort extends AstNode {
    @NotNull
    private final Expr expr;

    @Nullable
    private final Order order;

    @Nullable
    private final Nulls nulls;

    public Sort(@NotNull Expr expr, @Nullable Order order, @Nullable Nulls nulls) {
        this.expr = expr;
        this.order = order;
        this.nulls = nulls;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(expr);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSort(this, ctx);
    }

    @NotNull
    public Expr getExpr() {
        return this.expr;
    }

    @Nullable
    public Order getOrder() {
        return this.order;
    }

    @Nullable
    public Nulls getNulls() {
        return this.nulls;
    }
}
