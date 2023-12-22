package org.partiql.planner.internal.typer

import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
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
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
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
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.STRUCT
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

/**
 * Group all function implementations by their name, sorting by precedence.
 */
internal fun <T : FunctionSignature> List<T>.toFnMap(): FnMap<T> = this
    .distinctBy { it.specific }
    .sortedWith(fnPrecedence)
    .groupBy { it.name }

// Function precedence comparator
// 1. Fewest args first
// 2. Parameters are compared left-to-right
internal val fnPrecedence = Comparator<FunctionSignature> { fn1, fn2 ->
    // Compare number of arguments
    if (fn1.parameters.size != fn2.parameters.size) {
        return@Comparator fn1.parameters.size - fn2.parameters.size
    }
    // Compare operand type precedence
    for (i in fn1.parameters.indices) {
        val p1 = fn1.parameters[i]
        val p2 = fn2.parameters[i]
        val comparison = p1.compareTo(p2)
        if (comparison != 0) return@Comparator comparison
    }
    // unreachable?
    0
}

@OptIn(PartiQLValueExperimental::class)
internal fun FunctionParameter.compareTo(other: FunctionParameter): Int =
    comparePrecedence(this.type, other.type)

@OptIn(PartiQLValueExperimental::class)
internal fun comparePrecedence(t1: PartiQLValueType, t2: PartiQLValueType): Int {
    if (t1 == t2) return 0
    val p1 = precedence[t1]!!
    val p2 = precedence[t2]!!
    return p1 - p2
}

// This simply describes some precedence for ordering functions.
// This is not explicitly defined in the PartiQL Specification!!
// This does not imply the ability to CAST; this defines function resolution behavior.
@OptIn(PartiQLValueExperimental::class)
private val precedence: Map<PartiQLValueType, Int> = listOf(
    NULL,
    MISSING,
    BOOL,
    INT8,
    INT16,
    INT32,
    INT64,
    INT,
    DECIMAL,
    FLOAT32,
    FLOAT64,
    DECIMAL_ARBITRARY, // Arbitrary precision decimal has a higher precedence than FLOAT
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
