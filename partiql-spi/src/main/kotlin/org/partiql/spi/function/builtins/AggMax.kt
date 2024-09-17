// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.AggSignature
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.AccumulatorMax
import org.partiql.types.PType

internal object Agg_MAX__INT8__INT8 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.tinyint(),
        parameters = listOf(
            Parameter("value", PType.tinyint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT16__INT16 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.smallint(),
        parameters = listOf(
            Parameter("value", PType.smallint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT32__INT32 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("value", PType.integer()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT64__INT64 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("value", PType.bigint()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__INT__INT : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.numeric(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__FLOAT32__FLOAT32 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.real(),
        parameters = listOf(
            Parameter("value", PType.real()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__FLOAT64__FLOAT64 : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.doublePrecision(),
        parameters = listOf(
            Parameter("value", PType.doublePrecision()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMax()
}

internal object Agg_MAX__ANY__ANY : Aggregation {

    override val signature: AggSignature = AggSignature(
        name = "max",
        returns = PType.dynamic(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Aggregation.Accumulator = AccumulatorMax()
}
