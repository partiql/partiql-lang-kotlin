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
 * Represents SQL's SUBSTRING special form function (E021-06). E.g. {@code SUBSTRING(a FROM b FOR c)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprSubstring extends Expr {
    @NotNull
    private final Expr value;

    @Nullable
    private final Expr start;

    @Nullable
    private final Expr length;

    public ExprSubstring(@NotNull Expr value, @Nullable Expr start, @Nullable Expr length) {
        this.value = value;
        this.start = start;
        this.length = length;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        if (start != null) {
            kids.add(start);
        }
        if (length != null) {
            kids.add(length);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprSubstring(this, ctx);
    }

    @NotNull
    public Expr getValue() {
        return this.value;
    }

    @Nullable
    public Expr getStart() {
        return this.start;
    }

    @Nullable
    public Expr getLength() {
        return this.length;
    }
}
