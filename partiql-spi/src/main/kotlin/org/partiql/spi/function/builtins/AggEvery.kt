// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.AggSignature
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorEvery
import org.partiql.types.PType

internal object Agg_EVERY__BOOL__BOOL : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "every",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bool()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorEvery()
}

internal object Agg_EVERY__ANY__BOOL : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "every",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorEvery()
}
