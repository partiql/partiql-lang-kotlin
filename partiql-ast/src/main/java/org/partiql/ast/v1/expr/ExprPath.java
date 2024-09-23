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
public class ExprPath extends Expr {
    @NotNull
    public Expr root;

    @NotNull
    public List<ExprPathStep> steps;

    public ExprPath(@NotNull Expr root, @NotNull List<ExprPathStep> steps) {
        this.root = root;
        this.steps = steps;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(root);
        kids.addAll(steps);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprPath(this, ctx);
    }
}
