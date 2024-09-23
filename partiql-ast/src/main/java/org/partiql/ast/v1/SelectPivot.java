package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class SelectPivot extends Select {
    @NotNull
    public Expr key;

    @NotNull
    public Expr value;

    public SelectPivot(@NotNull Expr key, @NotNull Expr value) {
        this.key = key;
        this.value = value;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(key);
        kids.add(value);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSelectPivot(this, ctx);
    }
}
