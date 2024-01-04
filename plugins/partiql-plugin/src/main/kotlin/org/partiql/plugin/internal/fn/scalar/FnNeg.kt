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
internal object Fn_NEG__INT8__INT8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT8,
        parameters = listOf(FunctionParameter("value", INT8)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__INT16__INT16 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT16,
        parameters = listOf(FunctionParameter("value", INT16)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__INT32__INT32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT32,
        parameters = listOf(FunctionParameter("value", INT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__INT64__INT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT64,
        parameters = listOf(FunctionParameter("value", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__INT__INT : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT,
        parameters = listOf(FunctionParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FunctionParameter("value", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__FLOAT32__FLOAT32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = FLOAT32,
        parameters = listOf(FunctionParameter("value", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__FLOAT64__FLOAT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = FLOAT64,
        parameters = listOf(FunctionParameter("value", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function neg not implemented")
    }
}
