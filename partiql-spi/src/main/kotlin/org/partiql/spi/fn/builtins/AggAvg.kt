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
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(
            FnParameter("value", PType.typeTinyInt()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.typeDecimalArbitrary())
}

internal object Agg_AVG__INT16__INT16 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(
            FnParameter("value", PType.typeSmallInt()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.typeDecimalArbitrary())
}

internal object Agg_AVG__INT32__INT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(
            FnParameter("value", PType.typeInt()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.typeDecimalArbitrary())
}

internal object Agg_AVG__INT64__INT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(
            FnParameter("value", PType.typeBigInt()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.typeDecimalArbitrary())
}

internal object Agg_AVG__INT__INT : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeIntArbitrary()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.typeDecimalArbitrary())
}

internal object Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeDecimalArbitrary()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.typeDecimalArbitrary())
}

internal object Agg_AVG__FLOAT32__FLOAT32 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.typeReal(),
        parameters = listOf(
            FnParameter("value", PType.typeReal()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.typeReal())
}

internal object Agg_AVG__FLOAT64__FLOAT64 : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.typeDoublePrecision(),
        parameters = listOf(
            FnParameter("value", PType.typeDoublePrecision()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.typeDoublePrecision())
}

internal object Agg_AVG__ANY__ANY : Agg {

    override val signature: AggSignature = AggSignature(
        name = "avg",
        returns = PType.typeDecimalArbitrary(),
        parameters = listOf(
            FnParameter("value", PType.typeDynamic()),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator = AccumulatorAvg(PType.typeDecimalArbitrary())
}
