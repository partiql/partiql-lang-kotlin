package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.DatetimeField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprDateAdd extends Expr {
    @NotNull
    public DatetimeField field;

    @NotNull
    public Expr lhs;

    @NotNull
    public Expr rhs;

    public ExprDateAdd(@NotNull DatetimeField field, @NotNull Expr lhs, @NotNull Expr rhs) {
        this.field = field;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(lhs);
        kids.add(rhs);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprDateAdd(this, ctx);
    }
}
