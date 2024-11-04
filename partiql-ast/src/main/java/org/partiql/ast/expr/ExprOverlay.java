package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprOverlay extends Expr {
    @NotNull
    public final Expr value;

    @NotNull
    public final Expr placing;

    @NotNull
    public final Expr from;

    @Nullable
    public final Expr forLength;

    public ExprOverlay(@NotNull Expr value, @NotNull Expr placing, @NotNull Expr from, @Nullable Expr forLength) {
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
        if (forLength != null) {
            kids.add(forLength);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprOverlay(this, ctx);
    }
}
