package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.DataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprCast extends Expr {
    @NotNull
    public Expr value;

    @NotNull
    public DataType asType;

    public ExprCast(@NotNull Expr value, @NotNull DataType asType) {
        this.value = value;
        this.asType = asType;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprCast(this, ctx);
    }
}
