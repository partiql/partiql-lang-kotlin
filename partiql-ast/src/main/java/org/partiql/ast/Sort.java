package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Sort extends AstNode {
    @NotNull
    @Getter
    private final Expr expr;

    @Nullable
    @Getter
    private final Order order;

    @Nullable
    @Getter
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
}
