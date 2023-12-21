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
internal object FnOr0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BOOL), FunctionParameter("rhs", BOOL)),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function or not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnOr1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", MISSING), FunctionParameter("rhs", BOOL)),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function or not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnOr2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BOOL), FunctionParameter("rhs", MISSING)),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function or not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnOr3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "or",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", MISSING), FunctionParameter("rhs", MISSING)),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function or not implemented")
    }
}
