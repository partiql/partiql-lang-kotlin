package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprNullIf extends Expr {
    @NotNull
    public Expr value;

    @NotNull
    public Expr nullifier;

    public ExprNullIf(@NotNull Expr value, @NotNull Expr nullifier) {
        this.value = value;
        this.nullifier = nullifier;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        kids.add(nullifier);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprNullIf(this, ctx);
    }
}
