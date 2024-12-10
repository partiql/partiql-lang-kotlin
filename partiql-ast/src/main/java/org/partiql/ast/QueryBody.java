package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

public abstract class QueryBody extends AstNode {
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class SFW extends QueryBody {
        @NotNull
        public final Select select;

        @Nullable
        public final Exclude exclude;

        @NotNull
        public final From from;

        @Nullable
        public final Let let;

        @Nullable
        public final Expr where;

        @Nullable
        public final GroupBy groupBy;

        @Nullable
        public final Expr having;

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

    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class SetOp extends QueryBody {
        @NotNull
        public final org.partiql.ast.SetOp type;

        public final boolean isOuter;

        @NotNull
        public Expr lhs;

        @NotNull
        public Expr rhs;

        public SetOp(@NotNull org.partiql.ast.SetOp type, boolean isOuter, @NotNull Expr lhs, @NotNull Expr rhs) {
            this.type = type;
            this.isOuter = isOuter;
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
