package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Literal;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's literal expression. E.g. {@code 'string literal'}.
 */
@EqualsAndHashCode(callSuper = false)
public final class ExprLit extends Expr {
    @NotNull
    @Getter
    private final Literal lit;

    public ExprLit(@NotNull Literal lit) {
        this.lit = lit;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(lit);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprLit(this, ctx);
    }
}
