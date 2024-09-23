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
public class ExprValues extends Expr {
    @NotNull
    public List<Row> rows;

    public ExprValues(@NotNull List<Row> rows) {
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

    /**
     * TODO docs, equals, hashcode
     */
    public static class Row extends AstNode {
        @NotNull
        public List<Expr> values;

        public Row(@NotNull List<Expr> values) {
            this.values = values;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            return new ArrayList<>(values);
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprValuesRow(this, ctx);
        }
    }
}
