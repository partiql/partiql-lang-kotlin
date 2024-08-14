// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_IS_INT__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_int",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.dynamic())),
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
