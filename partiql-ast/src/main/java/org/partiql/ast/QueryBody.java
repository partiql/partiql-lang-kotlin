package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for contents within a query.
 */
public abstract class QueryBody extends AstNode {
    /**
     * Represents a SELECT-FROM-WHERE query.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class SFW extends QueryBody {
        @NotNull
        private final Select select;

        @Nullable
        private final Exclude exclude;

        @NotNull
        private final From from;

        @Nullable
        private final Let let;

        @Nullable
        private final Expr where;

        @Nullable
        private final GroupBy groupBy;

        @Nullable
        private final Expr having;

        @Nullable
        private final WindowClause windowClause;

        /**
         * Constructs a new SFW query body.
         * @param select the select clause
         * @param exclude the exclude clause
         * @param from the from clause
         * @param let the let clause
         * @param where the where clause
         * @param groupBy the group by clause
         * @param having the having clause
         * @param window the window clause
         */
        public SFW(@NotNull Select select, @Nullable Exclude exclude, @NotNull From from,
                   @Nullable Let let, @Nullable Expr where, @Nullable GroupBy groupBy,
                   @Nullable Expr having, @Nullable WindowClause window) {
            this.select = select;
            this.exclude = exclude;
            this.from = from;
            this.let = let;
            this.where = where;
            this.groupBy = groupBy;
            this.having = having;
            this.windowClause = window;
        }

        /**
         * Constructs a new SFW query body.
         * @param select the select clause
         * @param exclude the exclude clause
         * @param from the from clause
         * @param let the let clause
         * @param where the where clause
         * @param groupBy the group by clause
         * @param having the having clause
         */
        public SFW(@NotNull Select select, @Nullable Exclude exclude, @NotNull From from,
                   @Nullable Let let, @Nullable Expr where, @Nullable GroupBy groupBy, @Nullable Expr having) {
            this.select = select;
            this.exclude = exclude;
            this.from = from;
            this.let = let;
            this.where = where;
            this.groupBy = groupBy;
            this.having = having;
            this.windowClause = null;
        }

        /**
         * Returns the select clause.
         * @return the select clause
         */
        @NotNull
        public Select getSelect() {
            return this.select;
        }

        /**
         * Returns the exclude clause, if any.
         * @return the exclude clause, or null if not present
         */
        @Nullable
        public Exclude getExclude() {
            return this.exclude;
        }

        /**
         * Returns the from clause.
         * @return the from clause
         */
        @NotNull
        public From getFrom() {
            return this.from;
        }

        /**
         * Returns the let clause, if any.
         * @return the let clause, or null if not present
         */
        @Nullable
        public Let getLet() {
            return this.let;
        }

        /**
         * Returns the where clause, if any.
         * @return the where clause, or null if not present
         */
        @Nullable
        public Expr getWhere() {
            return this.where;
        }

        /**
         * Returns the group by clause, if any.
         * @return the group by clause, or null if not present
         */
        @Nullable
        public GroupBy getGroupBy() {
            return this.groupBy;
        }

        /**
         * Returns the having clause, if any.
         * @return the having clause, or null if not present
         */
        @Nullable
        public Expr getHaving() {
            return this.having;
        }

        /**
         * Returns the window clause, if any.
         * @return the window clause, or null if not present
         */
        @Nullable
        public WindowClause getWindow() {
            return this.windowClause;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(select);
            if (exclude != null) kids.add(exclude);
            kids.add(from);
            if (let != null) kids.add(let);
            if (where != null) kids.add(where);
            if (groupBy != null) kids.add(groupBy);
            if (having != null) kids.add(having);
            if (windowClause != null) kids.add(windowClause);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitQueryBodySFW(this, ctx);
        }
    }

    /**
     * Represents a set operation query, which is composed of a set operation with two other expressions. This can
     * also be used to represent PartiQL's outer bag operator.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class SetOp extends QueryBody {
        @NotNull
        private final org.partiql.ast.SetOp type;

        private final boolean outer;

        @NotNull
        private final Expr lhs;

        @NotNull
        private final Expr rhs;

        public SetOp(@NotNull org.partiql.ast.SetOp type, boolean outer, @NotNull Expr lhs, @NotNull Expr rhs) {
            this.type = type;
            this.outer = outer;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(type);
            kids.add(lhs);
            kids.add(rhs);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitQueryBodySetOp(this, ctx);
        }

        @NotNull
        public org.partiql.ast.SetOp getType() {
            return this.type;
        }

        public boolean isOuter() {
            return this.outer;
        }

        @NotNull
        public Expr getLhs() {
            return this.lhs;
        }

        @NotNull
        public Expr getRhs() {
            return this.rhs;
        }
    }
}
