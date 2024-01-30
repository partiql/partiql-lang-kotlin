// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.INT32

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IS_TIME__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_time",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY),),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_time not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IS_TIME__BOOL_INT32_ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_time",
        returns = BOOL,
        parameters = listOf(
            FnParameter("type_parameter_1", BOOL),
            FnParameter("type_parameter_2", INT32),
            FnParameter("value", ANY),
        ),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_time not implemented")
    }
}
