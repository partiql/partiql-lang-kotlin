package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnIsString0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_string",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", ANY)),
        isNullCall = false,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_string not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnIsString1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_string",
        returns = BOOL,
        parameters = listOf(FunctionParameter("type_parameter_1", INT32), FunctionParameter("value", ANY)),
        isNullCall = false,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_string not implemented")
    }
}


