package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;
import org.partiql.ast.expr.ExprWindowFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            if (defaultValue != null) {
                kids.add(defaultValue);
            }
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
            if (defaultValue != null) {
                kids.add(defaultValue);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeLag(this, ctx);
        }
    }

    /**
     * Represents an aggregate function used as a window function, e.g. {@code SUM(x) OVER (...)}. The aggregate is
     * identified by name, as for {@link org.partiql.ast.expr.ExprCall}; the parser currently only produces COUNT, SUM,
     * AVG, MIN, and MAX. {@code COUNT(*)} is represented by the name {@code count} with no arguments (and no set
     * quantifier).
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class Aggregate extends WindowFunctionType {
        @NotNull
        private final Identifier function;

        @NotNull
        private final List<Expr> args;

        @Nullable
        private final SetQuantifier setq;

        /**
         * Constructs a new aggregate window function type.
         * @param function the name of the aggregate function
         * @param args the arguments of the aggregate; empty for {@code COUNT(*)}
         * @param setq the set quantifier (ALL/DISTINCT) of the aggregate, if specified
         * @throws NullPointerException if {@code function} or {@code args} is null
         * @throws IllegalArgumentException if {@code args} is empty and {@code setq} is not null
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public Aggregate(@NotNull Identifier function, @NotNull List<Expr> args, @Nullable SetQuantifier setq) {
            super();
            this.function = Objects.requireNonNull(function, "function");
            this.args = Objects.requireNonNull(args, "args");
            if (args.isEmpty() && setq != null) {
                throw new IllegalArgumentException("An aggregate without arguments (e.g. COUNT(*)) may not have a set quantifier");
            }
            this.setq = setq;
        }

        /**
         * Returns the name of the aggregate function.
         * @return the name of the aggregate function
         */
        @NotNull
        public Identifier getFunction() {
            return this.function;
        }

        /**
         * Returns the arguments of the aggregate; empty for {@code COUNT(*)}.
         * @return the arguments of the aggregate
         */
        @NotNull
        public List<Expr> getArgs() {
            return this.args;
        }

        /**
         * Returns the set quantifier of the aggregate, if specified.
         * @return the set quantifier of the aggregate, if specified
         */
        @Nullable
        public SetQuantifier getSetq() {
            return this.setq;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(function);
            kids.addAll(args);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeAggregate(this, ctx);
        }
    }

    /**
     * Represents the NTILE window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class Ntile extends WindowFunctionType {
        private final Expr numberOfTiles;

        /**
         * Constructs a new NTILE window function type.
         * @param numberOfTiles the number of tiles (buckets) to divide each partition into
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public Ntile(@NotNull Expr numberOfTiles) {
            super();
            this.numberOfTiles = numberOfTiles;
        }

        /**
         * Returns the number of tiles.
         * @return the number of tiles
         */
        @NotNull
        public Expr getNumberOfTiles() {
            return this.numberOfTiles;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(numberOfTiles);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeNtile(this, ctx);
        }
    }

    /**
     * Represents either the FIRST_VALUE or LAST_VALUE window function.
     * @see ExprWindowFunction#getFunctionType()
     * @see FirstValue
     * @see LastValue
     * @deprecated This feature is experimental and is subject to change.
     */
    @Deprecated
    public interface FirstOrLastValue {
        /**
         * Returns the value expression of the window function.
         * @return the value expression of the window function
         */
        @NotNull
        Expr getValue();

        /**
         * Returns the null treatment of the window function.
         * @return the null treatment of the window function
         */
        @Nullable
        WindowFunctionNullTreatment getNullTreatment();
    }

    /**
     * Represents the FIRST_VALUE window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class FirstValue extends WindowFunctionType implements FirstOrLastValue {
        private final Expr value;
        private final WindowFunctionNullTreatment nullTreatment;

        /**
         * Constructs a new FIRST_VALUE window function type.
         * @param value the value expression of the window function
         * @param nullTreatment the null treatment of the window function
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public FirstValue(@NotNull Expr value, @Nullable WindowFunctionNullTreatment nullTreatment) {
            super();
            this.value = value;
            this.nullTreatment = nullTreatment;
        }

        @NotNull
        @Override
        public Expr getValue() {
            return this.value;
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
            kids.add(value);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeFirstValue(this, ctx);
        }
    }

    /**
     * Represents the LAST_VALUE window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class LastValue extends WindowFunctionType implements FirstOrLastValue {
        private final Expr value;
        private final WindowFunctionNullTreatment nullTreatment;

        /**
         * Constructs a new LAST_VALUE window function type.
         * @param value the value expression of the window function
         * @param nullTreatment the null treatment of the window function
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public LastValue(@NotNull Expr value, @Nullable WindowFunctionNullTreatment nullTreatment) {
            super();
            this.value = value;
            this.nullTreatment = nullTreatment;
        }

        @NotNull
        @Override
        public Expr getValue() {
            return this.value;
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
            kids.add(value);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeLastValue(this, ctx);
        }
    }

    /**
     * Represents the NTH_VALUE window function.
     * @see ExprWindowFunction#getFunctionType()
     * @deprecated This feature is experimental and is subject to change.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class NthValue extends WindowFunctionType {
        private final Expr value;
        private final Expr nthRow;
        private final WindowFunctionFromFirstOrLast fromFirstOrLast;
        private final WindowFunctionNullTreatment nullTreatment;

        /**
         * Constructs a new NTH_VALUE window function type.
         * @param value the value expression of the window function
         * @param nthRow the (1-based) row of the window frame to return the value of
         * @param fromFirstOrLast whether to count from the first or the last row of the window frame; null if not specified
         * @param nullTreatment the null treatment of the window function; null if not specified
         * @deprecated This feature is experimental and is subject to change.
         */
        @Deprecated
        public NthValue(
                @NotNull Expr value,
                @NotNull Expr nthRow,
                @Nullable WindowFunctionFromFirstOrLast fromFirstOrLast,
                @Nullable WindowFunctionNullTreatment nullTreatment
        ) {
            super();
            this.value = value;
            this.nthRow = nthRow;
            this.fromFirstOrLast = fromFirstOrLast;
            this.nullTreatment = nullTreatment;
        }

        /**
         * Returns the value expression of the window function.
         * @return the value expression of the window function
         */
        @NotNull
        public Expr getValue() {
            return this.value;
        }

        /**
         * Returns the nth row expression of the window function.
         * @return the nth row expression of the window function
         */
        @NotNull
        public Expr getNthRow() {
            return this.nthRow;
        }

        /**
         * Returns the FROM FIRST/FROM LAST option of the window function.
         * @return the FROM FIRST/FROM LAST option of the window function; null if not specified
         */
        @Nullable
        public WindowFunctionFromFirstOrLast getFromFirstOrLast() {
            return this.fromFirstOrLast;
        }

        /**
         * Returns the null treatment of the window function.
         * @return the null treatment of the window function; null if not specified
         */
        @Nullable
        public WindowFunctionNullTreatment getNullTreatment() {
            return this.nullTreatment;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(value);
            kids.add(nthRow);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeNthValue(this, ctx);
        }
    }
}
