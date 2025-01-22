package org.partiql.eval.internal.operator

import org.partiql.eval.ExprValue
import org.partiql.spi.function.Agg

/**
 * Simple data class to hold a compile aggregation call.
 */
internal class Aggregate(
    val agg: Agg,
    val args: List<ExprValue>,
    val distinct: Boolean
)
