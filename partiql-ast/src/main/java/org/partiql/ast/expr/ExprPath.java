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
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprPath extends Expr {
    @NotNull
    public final Expr root;

    @Nullable
    public final PathStep next;

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
}
