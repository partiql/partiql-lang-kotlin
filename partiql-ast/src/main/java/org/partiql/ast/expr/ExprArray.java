package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an array constructor. E.g. {@code [1, 2, 3]}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprArray extends Expr {
    @NotNull
    private final List<Expr> values;

    public ExprArray(@NotNull List<Expr> values) {
        this.values = values;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return new ArrayList<>(values);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprArray(this, ctx);
    }

    @NotNull
    public List<Expr> getValues() {
        return this.values;
    }
}
