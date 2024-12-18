package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Query extends Statement {
    @NotNull
    @Getter
    private final Expr expr;

    public Query(@NotNull Expr expr) {
        this.expr = expr;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(expr);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitQuery(this, ctx);
    }
}
