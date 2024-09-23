package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ConstraintCheck extends ConstraintType {
    @NotNull
    public Expr expr;

    public ConstraintCheck(@NotNull Expr expr) {
        this.expr = expr;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(expr);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitConstraintCheck(this, ctx);
    }
}
