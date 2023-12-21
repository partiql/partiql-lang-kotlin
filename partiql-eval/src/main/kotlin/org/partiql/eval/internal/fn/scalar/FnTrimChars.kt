package org.partiql.eval.internal.fn.scalar


import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTrimChars0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim_chars",
        returns = STRING,
        parameters = listOf(FunctionParameter("value", STRING), FunctionParameter("chars", STRING)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_chars not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTrimChars1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim_chars",
        returns = SYMBOL,
        parameters = listOf(FunctionParameter("value", SYMBOL), FunctionParameter("chars", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_chars not implemented")
    }
}



@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnTrimChars2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "trim_chars",
        returns = CLOB,
        parameters = listOf(FunctionParameter("value", CLOB), FunctionParameter("chars", CLOB)),
        isNullCall = true,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function trim_chars not implemented")
    }
}


