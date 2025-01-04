package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Base abstract class for contents within a query.
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

        public SFW(@NotNull Select select, @Nullable Exclude exclude, @NotNull From from,
                   @Nullable Let let, @Nullable Expr where, @Nullable GroupBy groupBy, @Nullable Expr having) {
            this.select = select;
            this.exclude = exclude;
            this.from = from;
            this.let = let;
            this.where = where;
            this.groupBy = groupBy;
            this.having = having;
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
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitQueryBodySFW(this, ctx);
        }

        @NotNull
        public Select getSelect() {
            return this.select;
        }

        @Nullable
        public Exclude getExclude() {
            return this.exclude;
        }

        @NotNull
        public From getFrom() {
            return this.from;
        }

        @Nullable
        public Let getLet() {
            return this.let;
        }

        @Nullable
        public Expr getWhere() {
            return this.where;
        }

        @Nullable
        public GroupBy getGroupBy() {
            return this.groupBy;
        }

        @Nullable
        public Expr getHaving() {
            return this.having;
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
