package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's OVERLAY special form. E.g. {@code OVERLAY(value, placing, from, forLength)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprOverlay extends Expr {
    @NotNull
    @Getter
    private final Expr value;

    @NotNull
    @Getter
    private final Expr placing;

    @NotNull
    @Getter
    private final Expr from;

    @Nullable
    @Getter
    private final Expr forLength;

    public ExprOverlay(@NotNull Expr value, @NotNull Expr placing, @NotNull Expr from, @Nullable Expr forLength) {
        this.value = value;
        this.placing = placing;
        this.from = from;
        this.forLength = forLength;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
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
