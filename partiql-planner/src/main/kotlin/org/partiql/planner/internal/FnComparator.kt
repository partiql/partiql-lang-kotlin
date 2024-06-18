package org.partiql.planner.internal

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType
import org.partiql.types.PType.Kind
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

    private fun comparePrecedence(t1: PType, t2: PType): Int {
        if (t1 == t2) return 0
        val p1 = precedence[t1.kind]!!
        val p2 = precedence[t2.kind]!!
        return p1 - p2
    }

    /**
     * This simply describes some precedence for ordering functions.
     * This is not explicitly defined in the PartiQL Specification!!
     * This does not imply the ability to CAST; this defines function resolution behavior.
     * This excludes [Kind.ROW] and [Kind.UNKNOWN].
     */
    private val precedence: Map<Kind, Int> = listOf(
        Kind.BOOL,
        Kind.TINYINT,
        Kind.SMALLINT,
        Kind.INT,
        Kind.BIGINT,
        Kind.INT_ARBITRARY,
        Kind.DECIMAL,
        Kind.REAL,
        Kind.DOUBLE_PRECISION,
        Kind.DECIMAL_ARBITRARY, // Arbitrary precision decimal has a higher precedence than FLOAT
        Kind.CHAR,
        Kind.SYMBOL,
        Kind.STRING,
        Kind.CLOB,
        Kind.BLOB,
        Kind.DATE,
        Kind.TIME_WITHOUT_TZ,
        Kind.TIME_WITH_TZ,
        Kind.TIMESTAMP_WITHOUT_TZ,
        Kind.TIMESTAMP_WITH_TZ,
        Kind.LIST,
        Kind.SEXP,
        Kind.BAG,
        Kind.ROW,
        Kind.STRUCT,
        Kind.DYNAMIC,
    ).mapIndexed { precedence, type -> type to precedence }.toMap()
}
