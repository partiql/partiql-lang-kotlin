package org.partiql.planner.internal

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BAG
import org.partiql.value.PartiQLValueType.BINARY
import org.partiql.value.PartiQLValueType.BLOB
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.BYTE
import org.partiql.value.PartiQLValueType.CHAR
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.INTERVAL
import org.partiql.value.PartiQLValueType.LIST
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.PartiQLValueType.NULL
import org.partiql.value.PartiQLValueType.NUMERIC
import org.partiql.value.PartiQLValueType.NUMERIC_ARBITRARY
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.STRUCT
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

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

    private fun comparePrecedence(t1: PartiQLValueType, t2: PartiQLValueType): Int {
        if (t1 == t2) return 0
        val p1 = precedence[t1]!!
        val p2 = precedence[t2]!!
        return p1 - p2
    }

    // TODO: Make precedence using PartiQLType (parameterized)
    // This simply describes some precedence for ordering functions.
    // This is not explicitly defined in the PartiQL Specification!!
    // This does not imply the ability to CAST; this defines function resolution behavior.
    private val precedence: Map<PartiQLValueType, Int> = listOf(
        NULL,
        MISSING,
        BOOL,
        INT8,
        INT16,
        INT32,
        INT64,
        INT,
        NUMERIC,
        FLOAT32,
        FLOAT64,
        NUMERIC_ARBITRARY, // Arbitrary precision decimal has a higher precedence than FLOAT
        CHAR,
        STRING,
        CLOB,
        SYMBOL,
        BINARY,
        BYTE,
        BLOB,
        DATE,
        TIME,
        TIMESTAMP,
        INTERVAL,
        LIST,
        SEXP,
        BAG,
        STRUCT,
        ANY,
    ).mapIndexed { precedence, type -> type to precedence }.toMap()
}
