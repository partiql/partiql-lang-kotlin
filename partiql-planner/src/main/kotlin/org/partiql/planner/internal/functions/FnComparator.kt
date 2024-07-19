package org.partiql.planner.internal.functions

import org.partiql.planner.catalog.Function
import org.partiql.types.PType
import org.partiql.types.PType.Kind

/**
 * Function precedence comparator; this is not formally specified.
 *
 *  1. Fewest args first
 *  2. Parameters are compared left-to-right
 */
internal object FnComparator : Comparator<Function> {

    override fun compare(fn1: Function, fn2: Function): Int {
        // Compare number of arguments
        val p1 = fn1.getParameters()
        val p2 = fn2.getParameters()
        if (p1.size != p2.size) {
            return p1.size - p2.size
        }
        // Compare operand type precedence
        for (i in p2.indices) {
            val arg1 = p1[i]
            val arg2 = p2[i]
            val comparison = arg1.compareTo(arg2)
            if (comparison != 0) return comparison
        }
        // unreachable?
        return 0
    }

    private fun Function.Parameter.compareTo(other: Function.Parameter): Int =
        comparePrecedence(this.type, other.type)

    private fun comparePrecedence(t1: PType.Kind, t2: PType.Kind): Int {
        if (t1 == t2) return 0
        val p1 = precedence[t1]!!
        val p2 = precedence[t2]!!
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
        Kind.VARCHAR,
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
