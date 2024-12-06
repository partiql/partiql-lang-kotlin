package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This specifies the data to be inserted.
 * @see Insert
 */
public abstract class InsertSource extends AstNode {

    /**
     * This specifies the data to be inserted from a subquery or expression.
     * @see Insert
     * @see InsertSource
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class FromSubquery extends InsertSource {
        // TODO: Equals and hashcode

        /**
         * TODO
         */
        @Nullable
        public final List<Identifier> columns;

        /**
         * TODO
         */
        @NotNull
        public final Expr expr;

        /**
         * TODO
         * @param columns TODO
         * @param expr TODO
         */
        public FromSubquery(@Nullable List<Identifier> columns, @NotNull Expr expr) {
            this.columns = columns;
            this.expr = expr;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (columns != null) {
                kids.addAll(columns);
            }
            kids.add(expr);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitInsertSourceFromSubquery(this, ctx);
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
        // TODO: Equals and hashcode

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitInsertSourceFromDefault(this, ctx);
        }
    }
}
