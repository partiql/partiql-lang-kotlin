// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.builtins.internal.AccumulatorEvery
import org.partiql.types.PType

internal object Agg_EVERY__BOOL__BOOL : Agg {

    override val signature: AggSignature = AggSignature(
        name = "every",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBool()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorEvery()
}

internal object Agg_EVERY__ANY__BOOL : Agg {

    override val signature: AggSignature = AggSignature(
        name = "every",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorEvery()
}
