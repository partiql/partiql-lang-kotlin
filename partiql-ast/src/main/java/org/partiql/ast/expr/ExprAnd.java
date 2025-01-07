package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's logical AND operator. E.g. {@code a AND b}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprAnd extends Expr {
    @NotNull
    private final Expr lhs;

    @NotNull
    private final Expr rhs;

    public ExprAnd(@NotNull Expr lhs, @NotNull Expr rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(lhs);
        kids.add(rhs);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprAnd(this, ctx);
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
