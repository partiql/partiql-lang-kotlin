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
internal object FnConcat0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "concat",
        returns = STRING,
        parameters = listOf(FunctionParameter("lhs", STRING), FunctionParameter("rhs", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function concat not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnConcat1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "concat",
        returns = SYMBOL,
        parameters = listOf(FunctionParameter("lhs", SYMBOL), FunctionParameter("rhs", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function concat not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnConcat2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "concat",
        returns = CLOB,
        parameters = listOf(FunctionParameter("lhs", CLOB), FunctionParameter("rhs", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function concat not implemented")
    }
}
