package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnUpper0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "upper",
        returns = STRING,
        parameters = listOf(FunctionParameter("value", STRING)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function upper not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnUpper1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "upper",
        returns = SYMBOL,
        parameters = listOf(FunctionParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function upper not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnUpper2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "upper",
        returns = CLOB,
        parameters = listOf(FunctionParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function upper not implemented")
    }
}


