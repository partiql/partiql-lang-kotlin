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
 * @deprecated This feature is experimental and is subject to change.
 */
@Deprecated
public abstract class WindowFunctionType extends AstNode {

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>();
    }

    /**
     * The RANK window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class Rank extends WindowFunctionType {
        /**
         * Constructs a RANK window function type.
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public Rank() {}

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeRank(this, ctx);
        }
    }

    /**
     * The DENSE RANK window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class DenseRank extends WindowFunctionType {
        /**
         * Constructs a DENSE RANK window function type.
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public DenseRank() {}

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeDenseRank(this, ctx);
        }
    }

    /**
     * The PERCENT RANK window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class PercentRank extends WindowFunctionType {
        /**
         * Constructs a PERCENT RANK window function type.
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public PercentRank() {}

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypePercentRank(this, ctx);
        }
    }

    /**
     * The CUME_DIST window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class CumeDist extends WindowFunctionType {
        /**
         * Constructs a CUME_DIST window function.
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
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
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class RowNumber extends WindowFunctionType {

        /**
         * Constructs a new ROW_NUMBER window function.
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
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
     * @see Lead
     * @see Lag
     * @deprecated This feature is experimental and is subject to change.
     */
    @Deprecated
    public interface LeadOrLag {
        /**
         * Returns the extent of the window function.
         * @return the extent of the window function
         */
        @NotNull
        Expr getExtent();

        /**
         * Returns the offset of the window function.
         * @return the offset of the window function
         */
        @Nullable
        Long getOffset();

        /**
         * Returns the default value of the window function.
         * @return the default value of the window function
         */
        @Nullable
        Expr getDefaultValue();

        /**
         * Returns the null treatment of the window function.
         * @return the null treatment of the window function
         */
        @Nullable
        WindowFunctionNullTreatment getNullTreatment();
    }

    /**
     * Represents the LEAD window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class Lead extends WindowFunctionType implements LeadOrLag {
        private final Expr extent;
        private final Long offset;
        private final Expr defaultValue;
        private final WindowFunctionNullTreatment nullTreatment;
        /**
         * Constructs a new LEAD window function type.
         * @param extent the extent of the window function
         * @param offset the offset of the window function
         * @param defaultValue the default value of the window function
         * @param nullTreatment the null treatment of the window function
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public Lead(
                @NotNull Expr extent,
                @Nullable Long offset,
                @Nullable Expr defaultValue,
                @Nullable WindowFunctionNullTreatment nullTreatment
        ) {
            super();
            this.extent = extent;
            this.offset = offset;
            this.defaultValue = defaultValue;
            this.nullTreatment = nullTreatment;
        }

        @NotNull
        @Override
        public Expr getExtent() {
            return this.extent;
        }

        @Nullable
        @Override
        public Long getOffset() {
            return this.offset;
        }

        @Nullable
        @Override
        public Expr getDefaultValue() {
            return this.defaultValue;
        }

        @Nullable
        @Override
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
            return visitor.visitWindowFunctionTypeLead(this, ctx);
        }
    }

    /**
     * Represents the LAG window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class Lag extends WindowFunctionType implements LeadOrLag {
        private final Expr extent;
        private final Long offset;
        private final Expr defaultValue;
        private final WindowFunctionNullTreatment nullTreatment;
        /**
         * Constructs a new LAG window function type.
         * @param extent the extent of the window function
         * @param offset the offset of the window function
         * @param defaultValue the default value of the window function
         * @param nullTreatment the null treatment of the window function
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public Lag(
                @NotNull Expr extent,
                @Nullable Long offset,
                @Nullable Expr defaultValue,
                @Nullable WindowFunctionNullTreatment nullTreatment
        ) {
            super();
            this.extent = extent;
            this.offset = offset;
            this.defaultValue = defaultValue;
            this.nullTreatment = nullTreatment;
        }

        @NotNull
        @Override
        public Expr getExtent() {
            return this.extent;
        }

        @Nullable
        @Override
        public Long getOffset() {
            return this.offset;
        }

        @Nullable
        @Override
        public Expr getDefaultValue() {
            return this.defaultValue;
        }

        @Nullable
        @Override
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
            return visitor.visitWindowFunctionTypeLag(this, ctx);
        }
    }
}
