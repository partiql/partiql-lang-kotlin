// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.STRING

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CURRENT_USER____STRING : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "current_user",
        returns = STRING,
        parameters = listOf(),
        isNullCall = false,
        isNullable = true,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function current_user not implemented")
    }
}
