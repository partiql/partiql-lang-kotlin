package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprCoalesce extends Expr {
    @NotNull
    @Getter
    private final List<Expr> args;

    public ExprCoalesce(@NotNull List<Expr> args) {
        this.args = args;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return new ArrayList<>(args);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprCoalesce(this, ctx);
    }
}
