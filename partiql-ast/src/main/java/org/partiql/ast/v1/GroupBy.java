package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class GroupBy extends AstNode {
    @NotNull
    public GroupByStrategy strategy;

    @NotNull
    public List<Key> keys;

    @Nullable
    public Identifier asAlias;

    public GroupBy(@NotNull GroupByStrategy strategy, @NotNull List<Key> keys, @Nullable Identifier asAlias) {
        this.strategy = strategy;
        this.keys = keys;
        this.asAlias = asAlias;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
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
    public static class Key extends AstNode {
        @NotNull
        public Expr expr;

        @Nullable
        public Identifier asAlias;

        public Key(@NotNull Expr expr, @Nullable Identifier asAlias) {
        this.expr = expr;
        this.asAlias = asAlias;
    }

        @NotNull
        @Override
        public Collection<AstNode> children() {
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
