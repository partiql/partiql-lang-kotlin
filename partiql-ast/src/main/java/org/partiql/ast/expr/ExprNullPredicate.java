package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's IS [NOT] NULL predicate (8.7). E.g. {@code col1 IS NULL}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprNullPredicate extends Expr {
    @NotNull
    private final Expr value;

    private final boolean not;

    public ExprNullPredicate(@NotNull Expr value, boolean not) {
        this.value = value;
        this.not = not;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprNullPredicate(this, ctx);
    }

    @NotNull
    public Expr getValue() {
        return this.value;
    }

    public boolean isNot() {
        return this.not;
    }
}
