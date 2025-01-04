package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's SELECT VALUE clause.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class SelectValue extends Select {
    @NotNull
    private final Expr constructor;

    @Nullable
    private final SetQuantifier setq;

    public SelectValue(@NotNull Expr constructor, @Nullable SetQuantifier setq) {
        this.constructor = constructor;
        this.setq = setq;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(constructor);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSelectValue(this, ctx);
    }

    @NotNull
    public Expr getConstructor() {
        return this.constructor;
    }

    @Nullable
    public SetQuantifier getSetq() {
        return this.setq;
    }
}
