// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.Aggregation
import org.partiql.spi.fn.Parameter
import org.partiql.spi.fn.builtins.internal.AccumulatorAnySome
import org.partiql.types.PType

internal object Agg_SOME__BOOL__BOOL : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "some",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bool()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAnySome()
}

internal object Agg_SOME__ANY__BOOL : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "some",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAnySome()
}
