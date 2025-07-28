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
     * The RANK window function.
     * @see ExprWindowFunction#getFunctionType()
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Rank extends WindowFunctionType {
        private int _type;

        /**
         * The plain RANK variant.
         */
        public static int RANK = 0;

        /**
         * The DENSE RANK variant.
         */
        public static int DENSE_RANK = 1;

        /**
         * The PERCENT RANK variant.
         */
        public static int PERCENT_RANK = 2;

        /**
         * See the static fields of the {@link Rank} class.
         * @return the type of rank function.
         */
        public int getType() {
            return _type;
        }

        /**
         * Constructs a RANK function.
         * @param type the type of the RANK function. See the static fields in the {@link Rank} class.
         */
        public Rank(int type) {
            this._type = type;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeRank(this, ctx);
        }
    }

    /**
     * The CUME_DIST window function.
     * @see ExprWindowFunction#getFunctionType()
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class CumeDist extends WindowFunctionType {
        /**
         * Constructs a CUME_DIST window function.
         */
        public CumeDist() {}

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeCumeDist(this, ctx);
        }
    }

    /**
     * The ROW_NUMBER window function.
     * @see ExprWindowFunction#getFunctionType()
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class RowNumber extends WindowFunctionType {

        /**
         * Constructs a new ROW_NUMBER window function.
         */
        public RowNumber() {}

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeRowNumber(this, ctx);
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
