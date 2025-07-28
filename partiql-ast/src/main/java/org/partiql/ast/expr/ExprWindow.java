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
 * Represents a window function expression. E.g. {@code LAG (sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp.date}.
 * @deprecated This is replaced by {@link ExprWindowFunction}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
@Deprecated
public final class ExprWindow extends Expr {
    @NotNull
    private final WindowFunction windowFunction;

    @NotNull
    private final Expr expression;

    @Nullable
    private final Expr offset;

    @Nullable
    private final Expr defaultValue;

    @NotNull
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

    @NotNull
    public WindowFunction getWindowFunction() {
        return this.windowFunction;
    }

    @NotNull
    public Expr getExpression() {
        return this.expression;
    }

    @Nullable
    public Expr getOffset() {
        return this.offset;
    }

    @Nullable
    public Expr getDefaultValue() {
        return this.defaultValue;
    }

    @NotNull
    public Over getOver() {
        return this.over;
    }

    /**
     * Represents the OVER clause of a window function. E.g. {@code OVER (PARTITION BY <expr> ORDER BY <expr>)}.
     * @deprecated This is replaced by {@link org.partiql.ast.WindowReference}.
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static class Over extends AstNode {
        @NotNull
        private final List<Expr> partitions;

        @NotNull
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

        /**
          * Empty list represents no `PARTITION BY` specifications
         */
        @NotNull
        public List<Expr> getPartitions() {
            return this.partitions;
        }

        /**
         * Empty list represents no `ORDER BY` specifications
         */
        @NotNull
        public List<Sort> getSorts() {
            return this.sorts;
        }
    }
}
