// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
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

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__INT8_INT8_INT8__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT8),
            FunctionParameter("lower", INT8),
            FunctionParameter("upper", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__INT16_INT16_INT16__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT16),
            FunctionParameter("lower", INT16),
            FunctionParameter("upper", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__INT32_INT32_INT32__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT32),
            FunctionParameter("lower", INT32),
            FunctionParameter("upper", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__INT64_INT64_INT64__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT64),
            FunctionParameter("lower", INT64),
            FunctionParameter("upper", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__INT_INT_INT__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", INT),
            FunctionParameter("lower", INT),
            FunctionParameter("upper", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DECIMAL_ARBITRARY),
            FunctionParameter("lower", DECIMAL_ARBITRARY),
            FunctionParameter("upper", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__FLOAT32_FLOAT32_FLOAT32__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT32),
            FunctionParameter("lower", FLOAT32),
            FunctionParameter("upper", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__FLOAT64_FLOAT64_FLOAT64__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", FLOAT64),
            FunctionParameter("lower", FLOAT64),
            FunctionParameter("upper", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__STRING_STRING_STRING__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRING),
            FunctionParameter("lower", STRING),
            FunctionParameter("upper", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__SYMBOL_SYMBOL_SYMBOL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SYMBOL),
            FunctionParameter("lower", SYMBOL),
            FunctionParameter("upper", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__CLOB_CLOB_CLOB__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CLOB),
            FunctionParameter("lower", CLOB),
            FunctionParameter("upper", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__DATE_DATE_DATE__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", DATE),
            FunctionParameter("lower", DATE),
            FunctionParameter("upper", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__TIME_TIME_TIME__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIME),
            FunctionParameter("lower", TIME),
            FunctionParameter("upper", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BETWEEN__TIMESTAMP_TIMESTAMP_TIMESTAMP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", TIMESTAMP),
            FunctionParameter("lower", TIMESTAMP),
            FunctionParameter("upper", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}
