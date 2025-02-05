package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.OrderBy;
import org.partiql.ast.QueryBody;
import org.partiql.ast.With;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a query expression with an optional ORDER BY, LIMIT, and OFFSET clause.
 *
 * @see QueryBody.SFW
 * @see QueryBody.SetOp
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprQuerySet extends Expr {
    @NotNull
    private final QueryBody body;

    @Nullable
    private final OrderBy orderBy;

    @Nullable
    private final Expr limit;

    @Nullable
    private final Expr offset;

    @Nullable
    private final With with;

    public ExprQuerySet(@NotNull QueryBody body, @Nullable OrderBy orderBy, @Nullable Expr limit, @Nullable Expr offset) {
        this.body = body;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
        this.with = null;
    }

    public ExprQuerySet(@NotNull QueryBody body, @Nullable OrderBy orderBy, @Nullable Expr limit, @Nullable Expr offset, @Nullable With with) {
        this.body = body;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
        this.with = with;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(body);
        if (orderBy != null) {
            kids.add(orderBy);
        }
        if (limit != null) {
            kids.add(limit);
        }
        if (offset != null) {
            kids.add(offset);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprQuerySet(this, ctx);
    }

    @Nullable
    public With getWith() {
        return this.with;
    }

    @NotNull
    public QueryBody getBody() {
        return this.body;
    }

    @Nullable
    public OrderBy getOrderBy() {
        return this.orderBy;
    }

    @Nullable
    public Expr getLimit() {
        return this.limit;
    }

    @Nullable
    public Expr getOffset() {
        return this.offset;
    }
}
