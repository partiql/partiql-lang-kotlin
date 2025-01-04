package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's PIVOT clause.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class SelectPivot extends Select {
    @NotNull
    private final Expr key;

    @NotNull
    private final Expr value;

    public SelectPivot(@NotNull Expr key, @NotNull Expr value) {
        this.key = key;
        this.value = value;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(key);
        kids.add(value);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSelectPivot(this, ctx);
    }

    @NotNull
    public Expr getKey() {
        return this.key;
    }

    @NotNull
    public Expr getValue() {
        return this.value;
    }
}
