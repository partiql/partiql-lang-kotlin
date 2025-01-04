package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * This represents SQL:1999's table value constructor.
 * <code>
 * &lt;table value constructor&gt; ::= VALUES &lt;row value expression list&gt;
 * </code>
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprValues extends Expr {
    // TODO: May not be an expr?
    // TODO: Tracking issue for VALUES and subqueries -- https://github.com/partiql/partiql-lang-kotlin/issues/1641.

    @NotNull
    private final List<Expr> rows;

    public ExprValues(@NotNull List<Expr> rows) {
        this.rows = rows;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return new ArrayList<>(rows);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprValues(this, ctx);
    }

    @NotNull
    public List<Expr> getRows() {
        return this.rows;
    }
}
