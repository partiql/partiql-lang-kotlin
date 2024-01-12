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


@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IS_INTERVAL__ANY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "is_interval",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY),),
        isNullCall = false,
        isNullable = false,
    )
    
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function is_interval not implemented")
    }
}


