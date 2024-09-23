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
public class ExprOverlay extends Expr {
    @NotNull
    public Expr value;

    @NotNull
    public Expr placing;

    @NotNull
    public Expr from;

    @NotNull
    public Expr forLength;

    public ExprOverlay(@NotNull Expr value, @NotNull Expr placing, @NotNull Expr from, @NotNull Expr forLength) {
        this.value = value;
        this.placing = placing;
        this.from = from;
        this.forLength = forLength;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        kids.add(placing);
        kids.add(from);
        kids.add(forLength);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprOverlay(this, ctx);
    }
}
