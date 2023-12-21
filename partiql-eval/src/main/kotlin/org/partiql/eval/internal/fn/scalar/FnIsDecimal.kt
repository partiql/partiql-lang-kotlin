package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnIsDecimal0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_decimal",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", ANY)),
        isNullCall = false,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_decimal not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnIsDecimal1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_decimal",
        returns = BOOL,
        parameters = listOf(FunctionParameter("type_parameter_1", INT32), FunctionParameter("type_parameter_2", INT32), FunctionParameter("value", ANY)),
        isNullCall = false,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_decimal not implemented")
    }
}


