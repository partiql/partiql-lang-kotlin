package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a GROUP BY clause in SQL.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class GroupBy extends AstNode {
    @NotNull
    private final GroupByStrategy strategy;

    @NotNull
    private final List<Key> keys;

    @Nullable
    private final Identifier.Simple asAlias;

    public GroupBy(@NotNull GroupByStrategy strategy, @NotNull List<Key> keys, @Nullable Identifier.Simple asAlias) {
        this.strategy = strategy;
        this.keys = keys;
        this.asAlias = asAlias;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>(keys);
        if (asAlias != null) {
            kids.add(asAlias);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitGroupBy(this, ctx);
    }

    @NotNull
    public GroupByStrategy getStrategy() {
        return this.strategy;
    }

    @NotNull
    public List<Key> getKeys() {
        return this.keys;
    }

    @Nullable
    public Identifier.Simple getAsAlias() {
        return this.asAlias;
    }

    /**
     * Represents a single key in a GROUP BY clause with an optional alias.
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Key extends AstNode {
        @NotNull
        private final Expr expr;

        @Nullable
        private final Identifier.Simple asAlias;

        public Key(@NotNull Expr expr, @Nullable Identifier.Simple asAlias) {
            this.expr = expr;
            this.asAlias = asAlias;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(expr);
            if (asAlias != null) {
                kids.add(asAlias);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGroupByKey(this, ctx);
        }

        @NotNull
        public Expr getExpr() {
            return this.expr;
        }

        @Nullable
        public Identifier.Simple getAsAlias() {
            return this.asAlias;
        }
    }
}
