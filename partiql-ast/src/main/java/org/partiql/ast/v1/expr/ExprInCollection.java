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
public class ExprInCollection extends Expr {
    @NotNull
    public Expr lhs;

    @NotNull
    public Expr rhs;

    public boolean not;

    public ExprInCollection(@NotNull Expr lhs, @NotNull Expr rhs, boolean not) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.not = not;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(lhs);
        kids.add(rhs);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprInCollection(this, ctx);
    }
}
