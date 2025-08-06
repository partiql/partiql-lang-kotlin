package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's OVERLAPS predicate. E.g. {@code (date1, date2) OVERLAPS (date3, date4)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprOverlaps extends Expr {
    @NotNull
    private final Expr lhs;

    @NotNull
    private final Expr rhs;

    public ExprOverlaps(@NotNull Expr lhs, @NotNull Expr rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(lhs);
        kids.add(rhs);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprOverlaps(this, ctx);
    }

    @NotNull
    public Expr getLhs() {
        return this.lhs;
    }

    @NotNull
    public Expr getRhs() {
        return this.rhs;
    }
}