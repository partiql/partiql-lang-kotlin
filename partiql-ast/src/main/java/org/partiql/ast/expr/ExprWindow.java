package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprWindow extends Expr {
    @NotNull
    public final WindowFunction windowFunction;

    @NotNull
    public final Expr expression;

    @Nullable
    public final Expr offset;

    @Nullable
    public final Expr defaultValue;

    @NotNull
    public final Over over;

    public ExprWindow(@NotNull WindowFunction windowFunction, @NotNull Expr expression, @Nullable Expr offset, @Nullable Expr defaultValue, @NotNull Over over) {
        this.windowFunction = windowFunction;
        this.expression = expression;
        this.offset = offset;
        this.defaultValue = defaultValue;
        this.over = over;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
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
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Over extends AstNode {
        @Nullable
        public final List<Expr> partitions;

        @Nullable
        public final List<Sort> sorts;

        public Over(@Nullable List<Expr> partitions, @Nullable List<Sort> sorts) {
        this.partitions = partitions;
        this.sorts = sorts;
    }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
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
