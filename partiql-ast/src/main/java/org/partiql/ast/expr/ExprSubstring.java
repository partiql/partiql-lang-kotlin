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
 * Represents SQL's SUBSTRING special form function (E021-06). E.g. {@code SUBSTRING(a FROM b FOR c)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprSubstring extends Expr {
    @NotNull
    @Getter
    private final Expr value;

    @Nullable
    @Getter
    private final Expr start;

    @Nullable
    @Getter
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
}
