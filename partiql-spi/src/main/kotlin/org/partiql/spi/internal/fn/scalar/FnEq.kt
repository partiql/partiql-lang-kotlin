// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.internal.fn.scalar

import org.partiql.spi.connector.ConnectorFunction
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BAG
import org.partiql.value.PartiQLValueType.BINARY
import org.partiql.value.PartiQLValueType.BLOB
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.BYTE
import org.partiql.value.PartiQLValueType.CHAR
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.INTERVAL
import org.partiql.value.PartiQLValueType.LIST
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.PartiQLValueType.NULL
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.STRUCT
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__ANY_ANY__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", ANY),
            FunctionParameter("rhs", ANY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__BOOL_BOOL__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BOOL),
            FunctionParameter("rhs", BOOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__INT8_INT8__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT8),
            FunctionParameter("rhs", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__INT16_INT16__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT16),
            FunctionParameter("rhs", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__INT32_INT32__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT32),
            FunctionParameter("rhs", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__INT64_INT64__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT64),
            FunctionParameter("rhs", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__INT_INT__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT),
            FunctionParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__DECIMAL_DECIMAL__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DECIMAL),
            FunctionParameter("rhs", DECIMAL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DECIMAL_ARBITRARY),
            FunctionParameter("rhs", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__FLOAT32_FLOAT32__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT32),
            FunctionParameter("rhs", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__FLOAT64_FLOAT64__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT64),
            FunctionParameter("rhs", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__CHAR_CHAR__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", CHAR),
            FunctionParameter("rhs", CHAR),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__STRING_STRING__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", STRING),
            FunctionParameter("rhs", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__SYMBOL_SYMBOL__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", SYMBOL),
            FunctionParameter("rhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__BINARY_BINARY__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BINARY),
            FunctionParameter("rhs", BINARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__BYTE_BYTE__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BYTE),
            FunctionParameter("rhs", BYTE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__BLOB_BLOB__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BLOB),
            FunctionParameter("rhs", BLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__CLOB_CLOB__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", CLOB),
            FunctionParameter("rhs", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__DATE_DATE__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DATE),
            FunctionParameter("rhs", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__TIME_TIME__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", TIME),
            FunctionParameter("rhs", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__TIMESTAMP_TIMESTAMP__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", TIMESTAMP),
            FunctionParameter("rhs", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__INTERVAL_INTERVAL__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INTERVAL),
            FunctionParameter("rhs", INTERVAL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__BAG_BAG__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BAG),
            FunctionParameter("rhs", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__LIST_LIST__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", LIST),
            FunctionParameter("rhs", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__SEXP_SEXP__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", SEXP),
            FunctionParameter("rhs", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__STRUCT_STRUCT__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", STRUCT),
            FunctionParameter("rhs", STRUCT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__NULL_NULL__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", NULL),
            FunctionParameter("rhs", NULL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_EQ__MISSING_MISSING__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", MISSING),
            FunctionParameter("rhs", MISSING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}
