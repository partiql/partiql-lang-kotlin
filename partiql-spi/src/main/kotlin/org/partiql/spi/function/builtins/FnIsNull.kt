// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

internal object Fn_IS_NULL__ANY__BOOL : Function {

    override val signature = FnSignature(
        name = "is_null",
        returns = PType.bool(),
        parameters = listOf(Parameter("value", PType.dynamic())),
        isNullable = false,
        isNullCall = false,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        if (args[0].isMissing) {
            return Datum.bool(true)
        }
        return Datum.bool(args[0].isNull)
    }
}
