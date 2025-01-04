package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's IN operator special form. E.g. {@code a IN (1, 2, 3)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprInCollection extends Expr {
    @NotNull
    private final Expr lhs;

    @NotNull
    private final Expr rhs;

    private final boolean not;

    public ExprInCollection(@NotNull Expr lhs, @NotNull Expr rhs, boolean not) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.not = not;
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
        return visitor.visitExprInCollection(this, ctx);
    }

    @NotNull
    public Expr getLhs() {
        return this.lhs;
    }

    @NotNull
    public Expr getRhs() {
        return this.rhs;
    }

    public boolean isNot() {
        return this.not;
    }
}
