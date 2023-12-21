package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnCurrentUser : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "current_user",
        returns = STRING,
        parameters = listOf(),
        isNullCall = false,
        isNullable = true,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function current_user not implemented")
    }
}


