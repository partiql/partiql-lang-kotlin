package org.partiql.planner.internal

import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
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
        val params1 = fn1.getParameters()
        val params2 = fn2.getParameters()
        // Compare number of arguments
        if (params1.size != params2.size) {
            return params1.size - params2.size
        }
        // Compare operand type precedence
        for (i in params1.indices) {
            val p1 = params1[i]
            val p2 = params2[i]
            val comparison = p1.compareTo(p2)
            if (comparison != 0) return comparison
        }
        // unreachable?
        return 0
    }

    private fun Parameter.compareTo(other: Parameter): Int =
        comparePrecedence(this.getType(), other.getType())

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
        Kind.INTEGER,
        Kind.BIGINT,
        Kind.NUMERIC,
        Kind.DECIMAL,
        Kind.REAL,
        Kind.DOUBLE,
        Kind.DECIMAL_ARBITRARY, // Arbitrary precision decimal has a higher precedence than FLOAT
        Kind.CHAR,
        Kind.VARCHAR,
        Kind.SYMBOL,
        Kind.STRING,
        Kind.CLOB,
        Kind.BLOB,
        Kind.DATE,
        Kind.TIME,
        Kind.TIMEZ,
        Kind.TIMESTAMP,
        Kind.TIMESTAMPZ,
        Kind.ARRAY,
        Kind.SEXP,
        Kind.BAG,
        Kind.ROW,
        Kind.STRUCT,
        Kind.DYNAMIC,
    ).mapIndexed { precedence, type -> type to precedence }.toMap()
}
