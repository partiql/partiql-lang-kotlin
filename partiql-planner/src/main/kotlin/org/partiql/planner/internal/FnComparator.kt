package org.partiql.planner.internal

import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType

/**
 * Function precedence comparator; this is not formally specified.
 *
 *  1. Fewest args first
 *  2. Parameters are compared left-to-right
 */
internal object FnComparator : Comparator<FnOverload> {

    override fun compare(fn1: FnOverload, fn2: FnOverload): Int {
        val params1 = fn1.signature.parameterTypes
        val params2 = fn2.signature.parameterTypes
        // Compare number of arguments
        if (fn1.signature.arity != fn2.signature.arity) {
            return fn1.signature.arity - fn2.signature.arity
        }
        // Compare operand type precedence
        for (i in params1.indices) {
            val p1 = params1[i]
            val p2 = params2[i]
            val comparison = comparePrecedence(p1, p2)
            if (comparison != 0) return comparison
        }
        // unreachable?
        return 0
    }

    private fun comparePrecedence(t1: PType, t2: PType): Int {
        if (t1 == t2) return 0
        val p1 = precedence[t1.code()]!!
        val p2 = precedence[t2.code()]!!
        return p1 - p2
    }

    /**
     * This simply describes some precedence for ordering functions.
     * This is not explicitly defined in the PartiQL Specification!!
     * This does not imply the ability to CAST; this defines function resolution behavior.
     * This excludes [PType.ROW] and [PType.UNKNOWN].
     */
    private val precedence: Map<Int, Int> = listOf(
        PType.BOOL,
        PType.TINYINT,
        PType.SMALLINT,
        PType.INTEGER,
        PType.BIGINT,
        PType.NUMERIC,
        PType.DECIMAL,
        PType.REAL,
        PType.DOUBLE,
        PType.CHAR,
        PType.VARCHAR,
        PType.STRING,
        PType.CLOB,
        PType.BLOB,
        PType.DATE,
        PType.TIME,
        PType.TIMEZ,
        PType.TIMESTAMP,
        PType.TIMESTAMPZ,
        PType.INTERVAL_YM, 
        PType.INTERVAL_DT, 
        PType.ARRAY,
        PType.BAG,
        PType.ROW,
        PType.STRUCT,
        PType.DYNAMIC,
    ).mapIndexed { precedence, type -> type to precedence }.toMap()
}
