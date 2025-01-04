package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @Nullable
    private final PathStep next;

    public ExprPath(@NotNull Expr root, @Nullable PathStep next) {
        this.root = root;
        this.next = next;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(root);
        if (next != null) {
            kids.add(next);
        }
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

    @Nullable
    public PathStep getNext() {
        return this.next;
    }
}
