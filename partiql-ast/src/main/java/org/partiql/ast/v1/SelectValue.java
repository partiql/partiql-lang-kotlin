package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class SelectValue extends Select {
    @NotNull
    public Expr constructor;

    @Nullable
    public SetQuantifier setq;

    public SelectValue(@NotNull Expr constructor, @Nullable SetQuantifier setq) {
        this.constructor = constructor;
        this.setq = setq;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(constructor);
            return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSelectValue(this, ctx);
    }
}
