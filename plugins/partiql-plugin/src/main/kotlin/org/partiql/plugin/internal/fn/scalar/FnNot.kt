package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.MISSING

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnNot0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "not",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BOOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function not not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnNot1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "not",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", MISSING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function not not implemented")
    }
}
