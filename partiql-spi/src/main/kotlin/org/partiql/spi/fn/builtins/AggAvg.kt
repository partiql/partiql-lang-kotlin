// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.Aggregation
import org.partiql.spi.fn.Parameter
import org.partiql.spi.fn.builtins.internal.AccumulatorAvg
import org.partiql.types.PType

internal object Agg_AVG__INT8__INT8 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            Parameter("value", PType.tinyint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__INT16__INT16 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            Parameter("value", PType.smallint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__INT32__INT32 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            Parameter("value", PType.integer()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__INT64__INT64 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            Parameter("value", PType.bigint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__INT__INT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAvg(PType.decimal())
}

internal object Agg_AVG__FLOAT32__FLOAT32 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.real(),
        parameters = listOf(
            Parameter("value", PType.real()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAvg(PType.real())
}

internal object Agg_AVG__FLOAT64__FLOAT64 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.doublePrecision(),
        parameters = listOf(
            Parameter("value", PType.doublePrecision()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAvg(PType.doublePrecision())
}

internal object Agg_AVG__ANY__ANY : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.decimal(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorAvg(PType.decimal())
}
