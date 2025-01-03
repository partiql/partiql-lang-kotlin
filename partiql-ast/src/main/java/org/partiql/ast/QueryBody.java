package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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
        @Getter
        private final Select select;

        @Nullable
        @Getter
        private final Exclude exclude;

        @NotNull
        @Getter
        private final From from;

        @Nullable
        @Getter
        private final Let let;

        @Nullable
        @Getter
        private final Expr where;

        @Nullable
        @Getter
        private final GroupBy groupBy;

        @Nullable
        @Getter
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
    }

    /**
     * Represents a set operation query, which is composed of a set operation with two other expressions. This can
     * also be used to represent PartiQL's outer bag operator.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class SetOp extends QueryBody {
        @NotNull
        @Getter
        private final org.partiql.ast.SetOp type;

        @Getter
        private final boolean outer;

        @NotNull
        @Getter
        private final Expr lhs;

        @NotNull
        @Getter
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
    }
}
