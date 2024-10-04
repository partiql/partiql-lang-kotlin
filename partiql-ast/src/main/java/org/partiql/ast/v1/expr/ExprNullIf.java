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
public class ExprNullIf extends Expr {
    @NotNull
    public Expr v1;

    @NotNull
    public Expr v2;

    public ExprNullIf(@NotNull Expr v1, @NotNull Expr v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(v1);
        kids.add(v2);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprNullIf(this, ctx);
    }
}
