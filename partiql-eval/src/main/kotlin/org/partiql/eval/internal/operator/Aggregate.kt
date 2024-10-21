package org.partiql.eval.internal.operator

import org.partiql.eval.ExprValue
import org.partiql.spi.function.Aggregation

/**
 * Simple data class to hold a compile aggregation call.
 */
internal class Aggregate(
    val agg: Aggregation,
    val args: List<ExprValue>,
    val distinct: Boolean
)
