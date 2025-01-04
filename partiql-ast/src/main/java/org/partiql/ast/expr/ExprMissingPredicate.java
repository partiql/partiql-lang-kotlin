package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's IS [NOT] MISSING predicate.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprMissingPredicate extends Expr {
    @NotNull
    private final Expr value;

    private final boolean not;

    public ExprMissingPredicate(@NotNull Expr value, boolean not) {
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
        return visitor.visitExprMissingPredicate(this, ctx);
    }

    @NotNull
    public Expr getValue() {
        return this.value;
    }

    public boolean isNot() {
        return this.not;
    }
}
