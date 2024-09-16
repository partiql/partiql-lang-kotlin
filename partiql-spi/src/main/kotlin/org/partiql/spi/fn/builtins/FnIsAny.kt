// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.types.PType

internal object Fn_IS_ANY__ANY__BOOL : Function {

    override val signature = FnSignature(
        name = "is_any",
        returns = PType.bool(),
        parameters = listOf(Parameter("value", PType.dynamic())),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        TODO("Function is_any not implemented")
    }
}
