package org.partiql.ast.v1.expr;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.OrderBy;
import org.partiql.ast.v1.QueryBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder
public class ExprQuerySet extends Expr {
    @NotNull
    public final QueryBody body;

    @Nullable
    public final OrderBy orderBy;

    @Nullable
    public final Expr limit;

    @Nullable
    public final Expr offset;

    public ExprQuerySet(@NotNull QueryBody body, @Nullable OrderBy orderBy, @Nullable Expr limit, @Nullable Expr offset) {
        this.body = body;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
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
}
