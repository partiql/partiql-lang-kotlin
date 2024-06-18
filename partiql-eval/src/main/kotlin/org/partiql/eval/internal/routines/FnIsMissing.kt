// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.MissingValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BOOL
import org.partiql.value.boolValue


internal object Fn_IS_MISSING__DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_missing",
        returns = BOOL,
        parameters = listOf(FnParameter("value", DYNAMIC)),
        isNullable = false,
        isNullCall = false,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return boolValue(args[0] is MissingValue)
    }
}
