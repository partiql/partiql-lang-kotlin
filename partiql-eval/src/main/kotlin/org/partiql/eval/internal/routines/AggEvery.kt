// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.connector.sql.builtins.internal.AccumulatorEvery
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BOOL


internal object Agg_EVERY__BOOL__BOOL : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "every",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorEvery()
}


internal object Agg_EVERY__DYNAMIC__BOOL : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "every",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DYNAMIC),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Accumulator = AccumulatorEvery()
}
