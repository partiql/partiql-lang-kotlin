package org.partiql.spi.connector.sql

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnAggregation
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * This is where we will register all SQL builtins. For now, we wrap the generated header to keep the diff small.
 */
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object SqlBuiltins {

    @JvmStatic
    private val scalars: List<Fn> = SqlHeader.functions.map {
        object : FnScalar {
            override val signature: FnSignature.Scalar = it
            override fun invoke(args: Array<PartiQLValue>): PartiQLValue = error("${it.name} not implemented")
        }
    }

    @JvmStatic
    private val aggregations: List<Fn> = SqlHeader.aggregations.map {
        object : FnAggregation {
            override val signature: FnSignature.Aggregation = it
            override fun accumulator(): FnAggregation.Accumulator = error("${it.name} not implemented")
        }
    }

    @JvmStatic
    val builtins: List<Fn> = scalars + aggregations
}
