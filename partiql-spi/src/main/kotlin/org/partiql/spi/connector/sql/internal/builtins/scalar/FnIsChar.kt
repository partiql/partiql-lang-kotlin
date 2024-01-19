// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.INT32

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IS_CHAR__ANY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "is_char",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY),),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_char not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IS_CHAR__INT32_ANY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "is_char",
        returns = BOOL,
        parameters = listOf(
            FnParameter("type_parameter_1", INT32),
            FnParameter("value", ANY),
        ),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_char not implemented")
    }
}
