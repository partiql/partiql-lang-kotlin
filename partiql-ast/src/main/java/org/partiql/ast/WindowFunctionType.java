package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;
import org.partiql.ast.expr.ExprWindowFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a window function type.
 * @see ExprWindowFunction#getFunctionType() 
 */
public abstract class WindowFunctionType extends AstNode {
    /**
     * A window function that takes no arguments.
     * @see ExprWindowFunction#getFunctionType()
     * @see WindowFunctionSimpleName
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class NoArg extends WindowFunctionType {
        private final WindowFunctionSimpleName name;

        /**
         * Constructs a no-argument window function type.
         * @param name the name of the window function
         */
        public NoArg(@NotNull WindowFunctionSimpleName name) {
            this.name = name;
        }

        /**
         * Returns the name of the window function.
         * @return the name of the window function
         */
        @NotNull
        public WindowFunctionSimpleName getName() {
            return this.name;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(name);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeNoArg(this, ctx);
        }
    }

    /**
     * Represents either the LAG or LEAD window function.
     * @see ExprWindowFunction#getFunctionType()
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class LeadOrLag extends WindowFunctionType {
        private final boolean isLead;
        private final Expr extent;
        private final Long offset;
        private final Expr defaultValue;
        private final WindowFunctionNullTreatment nullTreatment;

        /**
         * Constructs a new LEAD or LAG window function type.
         * @param isLead whether this is a LEAD or LAG window function
         * @param extent the extent of the window function
         * @param offset the offset of the window function
         * @param defaultValue the default value of the window function
         * @param nullTreatment the null treatment of the window function
         */
        public LeadOrLag(
                boolean isLead,
                @NotNull Expr extent,
                @Nullable Long offset,
                @Nullable Expr defaultValue,
                @Nullable WindowFunctionNullTreatment nullTreatment
        ) {
            this.isLead = isLead;
            this.extent = extent;
            this.offset = offset;
            this.defaultValue = defaultValue;
            this.nullTreatment = nullTreatment;
        }

        /**
         * Returns whether this is a LEAD or LAG window function.
         * @return whether this is a LEAD or LAG window function
         */
        public boolean isLead() {
            return this.isLead;
        }

        /**
         * Returns whether this is a LEAD or LAG window function.
         * @return whether this is a LEAD or LAG window function
         */
        public boolean isLag() {
            return !this.isLead;
        }

        /**
         * Returns the extent of the window function.
         * @return the extent of the window function
         */
        @NotNull
        public Expr getExtent() {
            return this.extent;
        }

        /**
         * Returns the offset of the window function.
         * @return the offset of the window function
         */
        @Nullable
        public Long getOffset() {
            return this.offset;
        }

        /**
         * Returns the default value of the window function.
         * @return the default value of the window function
         */
        @Nullable
        public Expr getDefaultValue() {
            return this.defaultValue;
        }

        /**
         * Returns the null treatment of the window function.
         * @return the null treatment of the window function
         */
        @Nullable
        public WindowFunctionNullTreatment getNullTreatment() {
            return this.nullTreatment;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(extent);
            kids.add(defaultValue);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeLagOrLead(this, ctx);
        }
    }
}
