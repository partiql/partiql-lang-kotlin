package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprSubstring extends Expr {
    @NotNull
    public Expr value;

    @Nullable
    public Expr start;

    @Nullable
    public Expr length;

    public ExprSubstring(@NotNull Expr value, @Nullable Expr start, @Nullable Expr length) {
        this.value = value;
        this.start = start;
        this.length = length;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
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
