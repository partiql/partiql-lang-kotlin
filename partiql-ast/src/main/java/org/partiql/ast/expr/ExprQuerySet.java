package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.OrderBy;
import org.partiql.ast.QueryBody;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprQuerySet extends Expr {
    @NotNull
    @Getter
    private final QueryBody body;

    @Nullable
    @Getter
    private final OrderBy orderBy;

    @Nullable
    @Getter
    private final Expr limit;

    @Nullable
    @Getter
    private final Expr offset;

    public ExprQuerySet(@NotNull QueryBody body, @Nullable OrderBy orderBy, @Nullable Expr limit, @Nullable Expr offset) {
        this.body = body;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
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
}
