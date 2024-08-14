// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.builtins.internal.AccumulatorSum
import org.partiql.types.PType

internal object Agg_SUM__INT8__INT8 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = PType.tinyint(),
        parameters = listOf(
            FnParameter("value", PType.tinyint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorSum(PType.tinyint())
}

internal object Agg_SUM__INT16__INT16 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = PType.smallint(),
        parameters = listOf(
            FnParameter("value", PType.smallint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorSum(PType.smallint())
}

internal object Agg_SUM__INT32__INT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = PType.integer(),
        parameters = listOf(
            FnParameter("value", PType.integer()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorSum(PType.integer())
}

internal object Agg_SUM__INT64__INT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = PType.bigint(),
        parameters = listOf(
            FnParameter("value", PType.bigint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorSum(PType.bigint())
}

internal object Agg_SUM__INT__INT : Agg {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = PType.numeric(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.numeric()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorSum(PType.numeric())
}

internal object Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.decimal()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorSum(PType.decimal())
}

internal object Agg_SUM__FLOAT32__FLOAT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = PType.real(),
        parameters = listOf(
            FnParameter("value", PType.real()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorSum(PType.real())
}

internal object Agg_SUM__FLOAT64__FLOAT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = PType.doublePrecision(),
        parameters = listOf(
            FnParameter("value", PType.doublePrecision()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorSum(PType.doublePrecision())
}

internal object Agg_SUM__ANY__ANY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "sum",
        returns = PType.dynamic(),
        parameters = listOf(
            FnParameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorSum()
}
