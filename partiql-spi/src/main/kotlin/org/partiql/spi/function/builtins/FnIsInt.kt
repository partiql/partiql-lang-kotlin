// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

internal object Fn_IS_INT__ANY__BOOL : Function {

    override val signature = FnSignature(
        name = "is_int",
        returns = PType.bool(),
        parameters = listOf(Parameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    private val INT_TYPES = setOf(
        PType.Kind.TINYINT,
        PType.Kind.SMALLINT,
        PType.Kind.INTEGER,
        PType.Kind.BIGINT,
        PType.Kind.NUMERIC
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg = args[0]
        return Datum.bool(arg.type.kind in INT_TYPES)
    }
}
