package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class GroupBy extends AstNode {
    @NotNull
    public final GroupByStrategy strategy;

    @NotNull
    public final List<Key> keys;

    @Nullable
    public final Identifier asAlias;

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
        public final Expr expr;

        @Nullable
        public final Identifier asAlias;

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
