package org.partiql.plan

import org.partiql.plan.rex.Rex
import org.partiql.spi.function.Aggregation

/**
 * TODO DOCUMENTATION
 */
public interface AggregateCall {

    public fun getAgg(): Aggregation

    public fun getArgs(): List<Rex>

    public fun isDistinct(): Boolean
}

/**
 * Internal standard implementation of [AggregateCall].
 *
 * DO NOT USE FINAL.
 *
 * @property agg
 * @property args
 * @property isDistinct
 */
internal class RelAggregateCallImpl(
    private var agg: Aggregation,
    private var args: List<Rex>,
    private var isDistinct: Boolean,
) : AggregateCall {
    override fun getAgg(): Aggregation = agg
    override fun getArgs(): List<Rex> = args
    override fun isDistinct(): Boolean = isDistinct
}
