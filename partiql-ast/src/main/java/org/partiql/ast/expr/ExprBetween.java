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
public final class ExprBetween extends Expr {
    @NotNull
    @Getter
    private final Expr value;

    @NotNull
    @Getter
    private final Expr from;

    @NotNull
    @Getter
    private final Expr to;

    @Getter
    private final boolean not;

    public ExprBetween(@NotNull Expr value, @NotNull Expr from, @NotNull Expr to, boolean not) {
        this.value = value;
        this.from = from;
        this.to = to;
        this.not = not;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        kids.add(from);
        kids.add(to);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprBetween(this, ctx);
    }
}
