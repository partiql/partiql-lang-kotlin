// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.internal.fn.scalar

import org.partiql.spi.connector.ConnectorFunction
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.INT32

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_IS_CHAR__ANY__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_char",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", ANY)),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_char not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, ConnectorFunctionExperimental::class)
internal object Fn_IS_CHAR__INT32_ANY__BOOL : ConnectorFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_char",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("type_parameter_1", INT32),
            FunctionParameter("value", ANY),
        ),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_char not implemented")
    }
}
