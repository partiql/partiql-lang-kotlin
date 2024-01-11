// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_TRIM_TRAILING__STRING__STRING : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim_trailing",
        returns = STRING,
        parameters = listOf(FunctionParameter("value", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_trailing not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_TRIM_TRAILING__SYMBOL__SYMBOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim_trailing",
        returns = SYMBOL,
        parameters = listOf(FunctionParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_trailing not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_TRIM_TRAILING__CLOB__CLOB : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim_trailing",
        returns = CLOB,
        parameters = listOf(FunctionParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_trailing not implemented")
    }
}