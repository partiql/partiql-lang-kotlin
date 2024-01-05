// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.timestampValue

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_UTCNOW____TIMESTAMP : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "utcnow",
        returns = TIMESTAMP,
        parameters = listOf(),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val now = TimestampWithTimeZone.nowZ()
        return timestampValue(now)
    }
}
