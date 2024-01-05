// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POS__INT8__INT8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "pos",
        returns = INT8,
        parameters = listOf(FunctionParameter("value", INT8)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POS__INT16__INT16 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "pos",
        returns = INT16,
        parameters = listOf(FunctionParameter("value", INT16)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POS__INT32__INT32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "pos",
        returns = INT32,
        parameters = listOf(FunctionParameter("value", INT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POS__INT64__INT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "pos",
        returns = INT64,
        parameters = listOf(FunctionParameter("value", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POS__INT__INT : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "pos",
        returns = INT,
        parameters = listOf(FunctionParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "pos",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FunctionParameter("value", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POS__FLOAT32__FLOAT32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "pos",
        returns = FLOAT32,
        parameters = listOf(FunctionParameter("value", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_POS__FLOAT64__FLOAT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "pos",
        returns = FLOAT64,
        parameters = listOf(FunctionParameter("value", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return args[0]
    }
}
