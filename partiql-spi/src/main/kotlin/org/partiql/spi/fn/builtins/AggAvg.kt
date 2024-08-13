// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.builtins.internal.AccumulatorAvg
import org.partiql.types.PType

internal object Agg_AVG__INT8__INT8 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            FnParameter("value", PType.typeTinyInt()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__INT16__INT16 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            FnParameter("value", PType.smallint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__INT32__INT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            FnParameter("value", PType.integer()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__INT64__INT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            FnParameter("value", PType.bigint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__INT__INT : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.numeric()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.decimal()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__FLOAT32__FLOAT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.real(),
        parameters = listOf(
            FnParameter("value", PType.real()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.real())
}

internal object Agg_AVG__FLOAT64__FLOAT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.doublePrecision(),
        parameters = listOf(
            FnParameter("value", PType.doublePrecision()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.doublePrecision())
}

internal object Agg_AVG__ANY__ANY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            FnParameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.decimal())
}
