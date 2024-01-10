// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.internal.fn.agg

import org.partiql.spi.connector.ConnectorFunction
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_AVG__INT8__INT8 : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT8,
        parameters = listOf(
            FunctionParameter("value", INT8),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_AVG__INT16__INT16 : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT16,
        parameters = listOf(
            FunctionParameter("value", INT16),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_AVG__INT32__INT32 : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT32,
        parameters = listOf(
            FunctionParameter("value", INT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_AVG__INT64__INT64 : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT64,
        parameters = listOf(
            FunctionParameter("value", INT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_AVG__INT__INT : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = INT,
        parameters = listOf(
            FunctionParameter("value", INT),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FunctionParameter("value", DECIMAL_ARBITRARY),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_AVG__FLOAT32__FLOAT32 : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = FLOAT32,
        parameters = listOf(
            FunctionParameter("value", FLOAT32),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Agg_AVG__FLOAT64__FLOAT64 : ConnectorFunction.Aggregation {

    override val signature = FunctionSignature.Aggregation(
        name = "avg",
        returns = FLOAT64,
        parameters = listOf(
            FunctionParameter("value", FLOAT64),
        ),
        isNullable = true,
        isDecomposable = true
    )

    override fun accumulator(): ConnectorFunction.Accumulator {
        TODO("Aggregation avg not implemented")
    }
}
