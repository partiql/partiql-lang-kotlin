package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.rex.Rex;
import org.partiql.spi.function.Agg;

import java.util.List;

/**
 * Represents a window function node.
 * @see WindowFunctionSignature
 * @deprecated This feature is experimental and is subject to change.
 */
@Deprecated
public final class WindowFunctionNode {

    private final WindowFunctionSignature signature;
    private final List<Rex> arguments;
    private final Agg aggregate;
    private final boolean distinct;

    /**
     * Constructs a new {@link WindowFunctionNode}.
     * @param signature the signature of the window function
     * @param arguments the arguments of the window function
     */
    public WindowFunctionNode(@NotNull WindowFunctionSignature signature, @NotNull List<Rex> arguments) {
        this(signature, arguments, null, false);
    }

    /**
     * Constructs a new {@link WindowFunctionNode} that may be an aggregate window function (e.g.
     * {@code SUM(x) OVER (...)}).
     * @param signature the signature of the window function
     * @param arguments the arguments of the window function (for an aggregate, the arguments of the aggregate)
     * @param aggregate the resolved aggregate function, or null if this is not an aggregate window function
     * @param distinct whether the aggregate is DISTINCT; must be false when {@code aggregate} is null
     */
    public WindowFunctionNode(
            @NotNull WindowFunctionSignature signature,
            @NotNull List<Rex> arguments,
            @Nullable Agg aggregate,
            boolean distinct
    ) {
        if (aggregate == null && distinct) {
            throw new IllegalArgumentException("DISTINCT is only applicable to aggregate window functions");
        }
        this.signature = signature;
        this.arguments = arguments;
        this.aggregate = aggregate;
        this.distinct = distinct;
    }

    /**
     * Returns the signature of the window function.
     * @return the signature of the window function
     */
    @NotNull
    public WindowFunctionSignature getSignature() {
        return signature;
    }

    /**
     * Returns the arguments of the window function.
     * @return the arguments of the window function
     */
    @NotNull
    public List<Rex> getArguments() {
        return arguments;
    }

    /**
     * Returns the resolved aggregate function if this is an aggregate window function (e.g. {@code SUM(x) OVER (...)});
     * otherwise, null. Aggregate window functions are evaluated using the same {@link Agg} (and accumulators) as
     * group aggregation.
     * @return the resolved aggregate function, or null if this is not an aggregate window function
     */
    @Nullable
    public Agg getAggregate() {
        return aggregate;
    }

    /**
     * Returns whether this is a DISTINCT aggregate window function.
     * @return true if this is a DISTINCT aggregate window function
     */
    public boolean isDistinct() {
        return distinct;
    }
}
