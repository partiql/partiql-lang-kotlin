package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's path expression. E.g. {@code a.b.c[*]}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprPath extends Expr {
    @NotNull
    private final Expr root;

    @NotNull
    private final List<PathStep> steps;

    public ExprPath(@NotNull Expr root, @NotNull List<PathStep> steps) {
        this.root = root;
        this.steps = steps;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(root);
        kids.addAll(steps);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprPath(this, ctx);
    }

    @NotNull
    public Expr getRoot() {
        return this.root;
    }

    @NotNull
    public List<PathStep> getSteps() {
        return this.steps;
    }
}
