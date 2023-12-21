package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Identifier
import org.partiql.plugin.PartiQLFunctions
import org.partiql.spi.connector.ConnectorFunctions
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
 * Function signature lookup by name.
 */
internal typealias FnMap<T> = Map<String, List<T>>

/**
 * This class is responsible for quickly looking up function signatures given a function name. We will expand upon
 * this to support the resolution of function names via the SQL path once ready.
 *
 * Note that this automatically includes the PartiQLMetadata as it would be wasteful to reproduce this every time.
 */
@PartiQLValueExperimental
internal class FnRegistry(private val metadata: Collection<ConnectorFunctions>) {

    /**
     * Calculate a queryable map of scalar function signatures.
     */
    private val uFns: FnMap<FunctionSignature.Scalar> = metadata.flatMap { it.functions }.toFnMap()

    /**
     * Calculate a queryable map of scalar function signatures from special forms.
     */
    private val uOps: FnMap<FunctionSignature.Scalar> = metadata.flatMap { it.operators }.toFnMap()

    /**
     * Calculate a queryable map of aggregation function signatures
     */
    private val uAggs: FnMap<FunctionSignature.Aggregation> = metadata.flatMap { it.aggregations }.toFnMap()

    /**
     * Return a list of all scalar function signatures matching the given identifier.
     */
    internal fun lookup(ref: Fn.Unresolved): List<FunctionSignature.Scalar> {
        val name = getFnName(ref.identifier)
        // builtin
        val pMap = if (ref.isHidden) pOps else pFns
        val pFns = pMap.getOrDefault(name, emptyList())
        // user-defined
        val uMap = if (ref.isHidden) uFns else uOps
        val uFns = uMap[name]
        if (uFns.isNullOrEmpty()) {
            return pFns
        }
        return pFns + uFns
    }

    /**
     * Return a list of all aggregation function signatures matching the given identifier.
     */
    internal fun lookup(ref: Agg.Unresolved): List<FunctionSignature.Aggregation> {
        val name = getFnName(ref.identifier)
        // builtin
        val pFns = pAggs.getOrDefault(name, emptyList())
        // user-defined
        val uFns = uAggs[name]
        if (uFns.isNullOrEmpty()) {
            return pFns
        }
        return pFns + uFns
    }

    /**
     * Returns the CAST function if exists, else null.
     */
    internal fun lookupCoercion(operand: PartiQLValueType, target: PartiQLValueType): FunctionSignature.Scalar? {
        val i = operand.ordinal
        val j = target.ordinal
        return pCasts.graph[i][j]?.castFn
    }

    internal fun isUnsafeCast(specific: String): Boolean = pCasts.unsafeCastSet.contains(specific)

    /**
     * Return a normalized function identifier for lookup in our list of function definitions.
     */
    private fun getFnName(identifier: Identifier): String = when (identifier) {
        is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
        is Identifier.Symbol -> identifier.symbol.lowercase()
    }

    companion object {

        /**
         * Static PartiQL casts information.
         */
        @JvmStatic
        val pCasts = TypeCasts.partiql()

        /**
         * Static PartiQL function signatures, don't recompute.
         */
        @JvmStatic
        val pFns = (PartiQLFunctions.functions + pCasts.relationships().map { it.castFn }).toFnMap()

        /**
         * Static PartiQL operator signatures, don't recompute.
         */
        @JvmStatic
        val pOps = PartiQLFunctions.operators.toFnMap()

        /**
         * Static PartiQL aggregation signatures, don't recompute.
         */
        @JvmStatic
        val pAggs = PartiQLFunctions.aggregations.toFnMap()

        /**
         * Group all function implementations by their name, sorting by precedence.
         */
        fun <T : FunctionSignature> List<T>.toFnMap(): FnMap<T> = this
            .distinctBy { it.specific }
            .sortedWith(fnPrecedence)
            .groupBy { it.name }

        // ====================================
        //  SORTING
        // ====================================

        // Function precedence comparator
        // 1. Fewest args first
        // 2. Parameters are compared left-to-right
        @JvmStatic
        private val fnPrecedence = Comparator<FunctionSignature> { fn1, fn2 ->
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

        private fun FunctionParameter.compareTo(other: FunctionParameter): Int =
            comparePrecedence(this.type, other.type)

        private fun comparePrecedence(t1: PartiQLValueType, t2: PartiQLValueType): Int {
            if (t1 == t2) return 0
            val p1 = precedence[t1]!!
            val p2 = precedence[t2]!!
            return p1 - p2
        }

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
    }
}
