// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.internal.fn.scalar

import org.partiql.spi.connector.ConnectorFunction
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__INT8_INT8__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT8),
            FunctionParameter("rhs", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__INT16_INT16__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT16),
            FunctionParameter("rhs", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__INT32_INT32__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT32),
            FunctionParameter("rhs", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__INT64_INT64__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT64),
            FunctionParameter("rhs", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__INT_INT__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT),
            FunctionParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DECIMAL_ARBITRARY),
            FunctionParameter("rhs", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__FLOAT32_FLOAT32__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT32),
            FunctionParameter("rhs", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__FLOAT64_FLOAT64__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT64),
            FunctionParameter("rhs", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__STRING_STRING__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", STRING),
            FunctionParameter("rhs", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__SYMBOL_SYMBOL__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", SYMBOL),
            FunctionParameter("rhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__CLOB_CLOB__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", CLOB),
            FunctionParameter("rhs", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__DATE_DATE__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DATE),
            FunctionParameter("rhs", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__TIME_TIME__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", TIME),
            FunctionParameter("rhs", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__TIMESTAMP_TIMESTAMP__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", TIMESTAMP),
            FunctionParameter("rhs", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_GTE__BOOL_BOOL__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gte",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BOOL),
            FunctionParameter("rhs", BOOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function gte not implemented")
    }
}
