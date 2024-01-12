// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.agg

import org.partiql.spi.fn.FnAggregation
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.*

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_AVG__INT8__INT8 : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "avg",
        returns = INT8,
        parameters = listOf(
            FnParameter("value", INT8),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_AVG__INT16__INT16 : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "avg",
        returns = INT16,
        parameters = listOf(
            FnParameter("value", INT16),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_AVG__INT32__INT32 : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "avg",
        returns = INT32,
        parameters = listOf(
            FnParameter("value", INT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_AVG__INT64__INT64 : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "avg",
        returns = INT64,
        parameters = listOf(
            FnParameter("value", INT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_AVG__INT__INT : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "avg",
        returns = INT,
        parameters = listOf(
            FnParameter("value", INT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "avg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_AVG__FLOAT32__FLOAT32 : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "avg",
        returns = FLOAT32,
        parameters = listOf(
            FnParameter("value", FLOAT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Agg_AVG__FLOAT64__FLOAT64 : FnAggregation {

    override val signature = FnSignature.Aggregation(
        name = "avg",
        returns = FLOAT64,
        parameters = listOf(
            FnParameter("value", FLOAT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): FnAggregation.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}
