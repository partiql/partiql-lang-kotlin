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
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprBag extends Expr {
    @NotNull
    @Getter
    private final List<Expr> values;

    public ExprBag(@NotNull List<Expr> values) {
        this.values = values;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return new ArrayList<>(values);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprBag(this, ctx);
    }
}
