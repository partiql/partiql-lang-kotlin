package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Re
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprWindow extends Expr {
    @NotNull
    @Getter
    private final WindowFunction windowFunction;

    @NotNull
    @Getter
    private final Expr expression;

    @Nullable
    @Getter
    private final Expr offset;

    @Nullable
    @Getter
    private final Expr defaultValue;

    @NotNull
    @Getter
    private final Over over;

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
        // Empty list represents no `PARTITION BY` specifications
        @NotNull
        @Getter
        private final List<Expr> partitions;

        // Empty list represents no `ORDER BY` specifications
        @NotNull
        @Getter
        private final List<Sort> sorts;

        public Over(@NotNull List<Expr> partitions, @NotNull List<Sort> sorts) {
        this.partitions = partitions;
        this.sorts = sorts;
    }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.addAll(partitions);
            kids.addAll(sorts);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprWindowOver(this, ctx);
        }
    }
}
