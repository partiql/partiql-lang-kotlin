package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * This specifies the data to be inserted.
 * @see Insert
 */
public abstract class InsertSource extends AstNode {

    /**
     * This specifies the data to be inserted from a subquery or expression. This represents (and generalizes)
     * SQL:1999's &lt;from subquery&gt; EBNF rule.
     *
     * @see Insert
     * @see InsertSource
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class FromExpr extends InsertSource {
        /**
         * TODO
         */
        @Nullable
        private final List<Identifier> columns;

        /**
         * TODO
         */
        @NotNull
        private final Expr expr;

        /**
         * TODO
         *
         * @param columns TODO
         * @param expr    TODO
         */
        public FromExpr(@Nullable List<Identifier> columns, @NotNull Expr expr) {
            this.columns = columns;
            this.expr = expr;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            if (columns != null) {
                kids.addAll(columns);
            }
            kids.add(expr);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitInsertSourceFromExpr(this, ctx);
        }

        @Nullable
        public List<Identifier> getColumns() {
            return this.columns;
        }

        @NotNull
        public Expr getExpr() {
            return this.expr;
        }
    }

    /**
     * This specifies the data to be inserted from the DEFAULT VALUES clause.
     * @see Insert
     * @see InsertSource
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class FromDefault extends InsertSource {

        /**
         * TODO
         */
        public FromDefault() {}

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitInsertSourceFromDefault(this, ctx);
        }
    }
}
