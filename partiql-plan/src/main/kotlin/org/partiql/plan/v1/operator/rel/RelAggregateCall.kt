package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.operator.rex.Rex
import org.partiql.spi.fn.Aggregation

/**
 * TODO DOCUMENTATION
 */
public interface RelAggregateCall {

    public fun getAgg(): Aggregation

    public fun getArgs(): List<Rex>

    public fun isDistinct(): Boolean
}

/**
 * Internal standard implementation of [RelAggregateCall].
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
) : RelAggregateCall {
    override fun getAgg(): Aggregation = agg
    override fun getArgs(): List<Rex> = args
    override fun isDistinct(): Boolean = isDistinct
}
