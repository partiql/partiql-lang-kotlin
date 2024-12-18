package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs
 * Corresponds to PartiQL's `IS [NOT] MISSING`.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprMissingPredicate extends Expr {
    @NotNull
    @Getter
    private final Expr value;

    @Getter
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
}
