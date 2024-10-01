package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.type.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprIsType extends Expr {
    @NotNull
    public Expr value;

    @NotNull
    public Type type;

    public boolean not;

    public ExprIsType(@NotNull Expr value, @NotNull Type type, boolean not) {
        this.value = value;
        this.type = type;
        this.not = not;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        kids.add(type);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprIsType(this, ctx);
    }
}
