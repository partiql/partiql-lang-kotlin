// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
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

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__ANY_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", ANY),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__ANY_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", ANY),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__ANY_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", ANY),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BOOL_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BOOL),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BOOL_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BOOL),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BOOL_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BOOL),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT8_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT8),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT8_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT8),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT8_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT8),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT16_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT16),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT16_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT16),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT16_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT16),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT32_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT32),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT32_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT32),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT32_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT32),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT64_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT64),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT64_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT64),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT64_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT64),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INT_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DECIMAL),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DECIMAL),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DECIMAL),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DECIMAL_ARBITRARY),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DECIMAL_ARBITRARY),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DECIMAL_ARBITRARY),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT32_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT32),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT32_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT32),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT32_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT32),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT64_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT64),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT64_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT64),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT64_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT64),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__CHAR_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CHAR),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__CHAR_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CHAR),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__CHAR_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CHAR),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__STRING_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRING),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__STRING_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRING),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__STRING_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRING),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__SYMBOL_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SYMBOL),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__SYMBOL_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SYMBOL),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__SYMBOL_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SYMBOL),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BINARY_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BINARY),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BINARY_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BINARY),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BINARY_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BINARY),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BYTE_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BYTE),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BYTE_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BYTE),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BYTE_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BYTE),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BLOB_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BLOB),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BLOB_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BLOB),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BLOB_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BLOB),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__CLOB_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CLOB),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__CLOB_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CLOB),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__CLOB_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CLOB),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__DATE_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DATE),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__DATE_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DATE),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__DATE_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DATE),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__TIME_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIME),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__TIME_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIME),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__TIME_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIME),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__TIMESTAMP_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIMESTAMP),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__TIMESTAMP_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIMESTAMP),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__TIMESTAMP_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIMESTAMP),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INTERVAL_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INTERVAL),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INTERVAL_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INTERVAL),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__INTERVAL_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INTERVAL),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BAG_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BAG),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BAG_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BAG),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__BAG_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", BAG),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__LIST_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", LIST),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__LIST_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", LIST),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__LIST_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", LIST),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__SEXP_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SEXP),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__SEXP_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SEXP),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__SEXP_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SEXP),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__STRUCT_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRUCT),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__STRUCT_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRUCT),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__STRUCT_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRUCT),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__NULL_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", NULL),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__NULL_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", NULL),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__NULL_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", NULL),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__MISSING_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", MISSING),
            FunctionParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__MISSING_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", MISSING),
            FunctionParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IN_COLLECTION__MISSING_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", MISSING),
            FunctionParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}
