// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_SUM__INT8__INT8 : Agg {

    override val signature = AggSignature(
        name = "sum",
        returns = INT8,
        parameters = listOf(
            FnParameter("value", INT8),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_SUM__INT16__INT16 : Agg {

    override val signature = AggSignature(
        name = "sum",
        returns = INT16,
        parameters = listOf(
            FnParameter("value", INT16),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_SUM__INT32__INT32 : Agg {

    override val signature = AggSignature(
        name = "sum",
        returns = INT32,
        parameters = listOf(
            FnParameter("value", INT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_SUM__INT64__INT64 : Agg {

    override val signature = AggSignature(
        name = "sum",
        returns = INT64,
        parameters = listOf(
            FnParameter("value", INT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_SUM__INT__INT : Agg {

    override val signature = AggSignature(
        name = "sum",
        returns = INT,
        parameters = listOf(
            FnParameter("value", INT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Agg {

    override val signature = AggSignature(
        name = "sum",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_SUM__FLOAT32__FLOAT32 : Agg {

    override val signature = AggSignature(
        name = "sum",
        returns = FLOAT32,
        parameters = listOf(
            FnParameter("value", FLOAT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_SUM__FLOAT64__FLOAT64 : Agg {

    override val signature = AggSignature(
        name = "sum",
        returns = FLOAT64,
        parameters = listOf(
            FnParameter("value", FLOAT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): Agg.Accumulator {
        TODO("Aggregation sum not implemented")
    }
}
