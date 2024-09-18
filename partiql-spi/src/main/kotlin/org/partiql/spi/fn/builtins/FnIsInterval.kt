// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_IS_INTERVAL__ANY__BOOL : Function {

    override val signature = FnSignature(
        name = "is_interval",
        returns = PType.bool(),
        parameters = listOf(Parameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("INTERVAL NOT SUPPORTED YET")
    }
}
