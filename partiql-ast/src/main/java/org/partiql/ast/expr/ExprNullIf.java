package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's NULLIF expression. E.g. {@code NULLIF(col1, col2)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprNullIf extends Expr {
    @NotNull
    private final Expr v1;

    @NotNull
    private final Expr v2;

    public ExprNullIf(@NotNull Expr v1, @NotNull Expr v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(v1);
        kids.add(v2);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprNullIf(this, ctx);
    }

    @NotNull
    public Expr getV1() {
        return this.v1;
    }

    @NotNull
    public Expr getV2() {
        return this.v2;
    }
}
