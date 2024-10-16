package org.partiql.ast.v1.expr;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder
public class ExprArray extends Expr {
    @NotNull
    public final List<Expr> values;

    public ExprArray(@NotNull List<Expr> values) {
        this.values = values;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        return new ArrayList<>(values);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprArray(this, ctx);
    }
}
