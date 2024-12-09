package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
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
public class ExprNullIf extends Expr {
    @NotNull
    public final Expr v1;

    @NotNull
    public final Expr v2;

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
}
