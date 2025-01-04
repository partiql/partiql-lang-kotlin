package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents operators represented with special symbol(s) that take one or two expressions as operands.
 * <p>
 * E.g. arithmetic operators {@code 1 + 2}, comparison operators {@code 1 < 2}, etc.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprOperator extends Expr {
    @NotNull
    private final String symbol;

    @Nullable
    private final Expr lhs;

    @NotNull
    private final Expr rhs;

    public ExprOperator(@NotNull String symbol, @Nullable Expr lhs, @NotNull Expr rhs) {
        this.symbol = symbol;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        if (lhs != null) {
            kids.add(lhs);
        }
        kids.add(rhs);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprOperator(this, ctx);
    }

    @NotNull
    public String getSymbol() {
        return this.symbol;
    }

    @Nullable
    public Expr getLhs() {
        return this.lhs;
    }

    @NotNull
    public Expr getRhs() {
        return this.rhs;
    }
}
