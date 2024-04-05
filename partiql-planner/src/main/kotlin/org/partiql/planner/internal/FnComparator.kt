package org.partiql.planner.internal

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLType
import org.partiql.value.PartiQLValueExperimental

/**
 * Function precedence comparator; this is not formally specified.
 *
 *  1. Fewest args first
 *  2. Parameters are compared left-to-right
 */
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object FnComparator : Comparator<FnSignature> {

    override fun compare(fn1: FnSignature, fn2: FnSignature): Int {
        // Compare number of arguments
        if (fn1.parameters.size != fn2.parameters.size) {
            return fn1.parameters.size - fn2.parameters.size
        }
        // Compare operand type precedence
        for (i in fn1.parameters.indices) {
            val p1 = fn1.parameters[i]
            val p2 = fn2.parameters[i]
            val comparison = p1.compareTo(p2)
            if (comparison != 0) return comparison
        }
        // unreachable?
        return 0
    }

    private fun FnParameter.compareTo(other: FnParameter): Int =
        comparePrecedence(this.type, other.type)

    private fun comparePrecedence(t1: PartiQLType, t2: PartiQLType): Int {
        if (t1 == t2) return 0
        val p1 = PartiQLType.PRECEDENCE_MAP[t1] ?: error("Could not find $t1 in precedence map.")
        val p2 = PartiQLType.PRECEDENCE_MAP[t2] ?: error("Could not find $t2 in precedence map.")
        return p1.compareTo(p2)
    }
}
