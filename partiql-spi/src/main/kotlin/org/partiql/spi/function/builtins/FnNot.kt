// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_NOT__BOOL__BOOL : Function {

    override val signature = FnSignature(
        name = "not",
        returns = PType.bool(),
        parameters = listOf(Parameter("value", PType.bool())),
        isNullable = false,
        isNullCall = true,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg = args[0]
        if (arg.isMissing) {
            return Datum.nullValue(PType.bool())
        }
        val value = arg.boolean
        return Datum.bool(value.not())
    }
}
