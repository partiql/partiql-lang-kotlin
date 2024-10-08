package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprWindow extends Expr {
    @NotNull
    public WindowFunction windowFunction;

    @NotNull
    public Expr expression;

    @Nullable
    public Expr offset;

    @Nullable
    public Expr defaultValue;

    @NotNull
    public Over over;

    public ExprWindow(@NotNull WindowFunction windowFunction, @NotNull Expr expression, @Nullable Expr offset, @Nullable Expr defaultValue, @NotNull Over over) {
        this.windowFunction = windowFunction;
        this.expression = expression;
        this.offset = offset;
        this.defaultValue = defaultValue;
        this.over = over;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(expression);
        if (offset != null) {
            kids.add(offset);
        }
        if (defaultValue != null) {
            kids.add(defaultValue);
        }
        kids.add(over);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprWindow(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Over extends AstNode {
        @Nullable
        public List<Expr> partitions;

        @Nullable
        public List<Sort> sorts;

        public Over(@Nullable List<Expr> partitions, @Nullable List<Sort> sorts) {
        this.partitions = partitions;
        this.sorts = sorts;
    }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (partitions != null) {
                kids.addAll(partitions);
            }
            if (sorts != null) {
                kids.addAll(sorts);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprWindowOver(this, ctx);
        }
    }
}
