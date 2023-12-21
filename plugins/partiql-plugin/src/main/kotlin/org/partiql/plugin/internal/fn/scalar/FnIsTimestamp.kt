package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.INT32

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnIsTimestamp0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_timestamp",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", ANY)),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_timestamp not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnIsTimestamp1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_timestamp",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("type_parameter_1", BOOL),
            FunctionParameter("type_parameter_2", INT32),
            FunctionParameter("value", ANY)
        ),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_timestamp not implemented")
    }
}
