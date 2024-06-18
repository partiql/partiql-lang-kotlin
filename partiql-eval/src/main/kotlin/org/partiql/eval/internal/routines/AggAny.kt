// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.eval.internal.Accumulator
import org.partiql.eval.internal.Aggregation
import org.partiql.eval.internal.routines.internal.isAbsent
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValueType

/**
 * Note that SOME is normalized to ANY.
 */
internal object AGG_ANY__BOOL__BOOL : Aggregation {

    override fun getKey(): String = "AGG_ANY__BOOL___BOOL"

    override fun accumulator() = object : Accumulator {

        private var result: Boolean? = null

        override fun next(args: Array<Datum>) {
            if (result == true) {
                return // short-circuit
            }
            val arg = args[0]
            if (arg.isAbsent()) {
                return
            }
            result = arg.boolean
        }

        override fun value(): Datum = when (result) {
            null -> Datum.nullValue(PartiQLValueType.BOOL)
            else -> Datum.boolValue(result!!)
        }
    }
}
