package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprCoalesce extends Expr {
    @NotNull
    public List<Expr> args;

    public ExprCoalesce(@NotNull List<Expr> args) {
        this.args = args;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        return new ArrayList<>(args);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprCoalesce(this, ctx);
    }
}
