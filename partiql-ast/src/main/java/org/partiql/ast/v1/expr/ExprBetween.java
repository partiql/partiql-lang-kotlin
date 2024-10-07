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
public class ExprBetween extends Expr {
    @NotNull
    public Expr value;

    @NotNull
    public Expr from;

    @NotNull
    public Expr to;

    public boolean not;

    public ExprBetween(@NotNull Expr value, @NotNull Expr from, @NotNull Expr to, boolean not) {
        this.value = value;
        this.from = from;
        this.to = to;
        this.not = not;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        kids.add(from);
        kids.add(to);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprBetween(this, ctx);
    }
}
