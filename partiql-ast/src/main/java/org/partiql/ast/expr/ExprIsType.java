package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.DataType;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 * TODO also support IS NULL, IS MISSING, IS UNKNOWN, IS TRUE, IS FALSE
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprIsType extends Expr {
    @NotNull
    public final Expr value;

    @NotNull
    public final DataType type;

    public final boolean not;

    public ExprIsType(@NotNull Expr value, @NotNull DataType type, boolean not) {
        this.value = value;
        this.type = type;
        this.not = not;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
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
