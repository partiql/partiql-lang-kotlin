package org.partiql.ast.v1.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 *  Also may not be an [Expr]?
 *  Tracking issue for VALUES and subqueries -- https://github.com/partiql/partiql-lang-kotlin/issues/1641.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprValues extends Expr {
    @NotNull
    public final List<ExprRowValue> rows;

    public ExprValues(@NotNull List<ExprRowValue> rows) {
        this.rows = rows;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        return new ArrayList<>(rows);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprValues(this, ctx);
    }
}
