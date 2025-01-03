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
 * Represents a GROUP BY clause in SQL.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class GroupBy extends AstNode {
    @NotNull
    @Getter
    private final GroupByStrategy strategy;

    @NotNull
    @Getter
    private final List<Key> keys;

    @Nullable
    @Getter
    private final Identifier asAlias;

    public GroupBy(@NotNull GroupByStrategy strategy, @NotNull List<Key> keys, @Nullable Identifier asAlias) {
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

    /**
     * TODO docs, equals, hashcode
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Key extends AstNode {
        @NotNull
        @Getter
        private final Expr expr;

        @Nullable
        @Getter
        private final Identifier asAlias;

        public Key(@NotNull Expr expr, @Nullable Identifier asAlias) {
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
    }
}
