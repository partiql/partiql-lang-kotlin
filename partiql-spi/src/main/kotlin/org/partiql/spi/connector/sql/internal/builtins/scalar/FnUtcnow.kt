// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.TIMESTAMP

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_UTCNOW____TIMESTAMP : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "utcnow",
        returns = TIMESTAMP,
        parameters = listOf(),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function utcnow not implemented")
    }
}
