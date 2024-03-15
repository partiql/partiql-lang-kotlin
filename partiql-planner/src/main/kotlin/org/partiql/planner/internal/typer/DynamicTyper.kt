@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.internal.typer

import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
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
 * Graph of super types for quick lookup because we don't have a tree.
 */
internal typealias SuperGraph = Array<Array<PartiQLValueType?>>

/**
 * For lack of a better name, this is the "dynamic typer" which implements the typing rules of SQL-99 9.3.
 *
 * SQL-99 9.3 Data types of results of aggregations (<case-when>, <collection value expression>, <query expression>)
 *  > https://web.cecs.pdx.edu/~len/sql1999.pdf#page=359
 *
 * Usage,
 *  To calculate the type of an "aggregation" create a new instance and "accumulate" each possible type.
 *  This is a pain with StaticType...
 */
@OptIn(PartiQLValueExperimental::class)
internal class DynamicTyper {

    private var supertype = NULL

    private var nullable = false
    private var missable = false
    private val allTypes = mutableSetOf<StaticType>()

    /**
     * Adds this
     */
    public fun accumulate(type: StaticType) {
        if (type is SingleType) {
            when (type) {
                is MissingType -> missable = true
                is NullType -> nullable = true
                else -> calculate(type)
            }
        } else {
            for (t in type.flatten().allTypes) {
                when (t) {
                    is MissingType -> missable = true
                    is NullType -> nullable = true
                    else -> calculate(t)
                }
            }
        }
    }

    public fun type(): StaticType {
        val modifiers = mutableSetOf<StaticType>()
        if (nullable) modifiers.add(StaticType.NULL)
        if (missable) modifiers.add(StaticType.MISSING)
        // If at top supertype, then return union of all accumulated types
        if (supertype == ANY) {
            return StaticType.unionOf(allTypes + modifiers)
        }
        // If a collection, then return union of all accumulated types as these coercion rules are not defined by SQL.
        if (supertype == STRUCT || supertype == BAG || supertype == LIST || supertype == SEXP) {
            return StaticType.unionOf(allTypes + modifiers)
        }
        // Otherwise, return the supertype
        val st = supertype.toNonNullStaticType()
        return if (modifiers.isEmpty()) {
            st
        } else {
            StaticType.unionOf(setOf(st) + modifiers).flatten()
        }
    }

    private fun calculate(type: StaticType) {
        allTypes.add(type)
        // Don't bother calculating the new supertype, we've already hit the top.
        if (supertype == ANY) return
        // Lookup and set the new minimum common supertype
        val t = type.toRuntimeType()
        supertype = when {
            supertype == NULL -> t // initialize
            t == NULL || t == MISSING || supertype == t -> return // skip
            else -> graph[supertype][t] ?: ANY // lookup, if missing then go to top.
        }
    }

    private operator fun <T> Array<T>.get(t: PartiQLValueType): T = get(t.ordinal)

    /**
     * !! IMPORTANT !!
     *
     * This is duplicated from the TypeLattice because that was removed in v1.0.0. I wanted to implement this as
     * a standalone component so that it is easy to merge (and later merge with CastTable) into v1.0.0.
     */
    companion object {

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)

        @JvmStatic
        private val N = PartiQLValueType.values().size

        @JvmStatic
        private fun edges(vararg edges: Pair<PartiQLValueType, PartiQLValueType>): Array<PartiQLValueType?> {
            val arr = arrayOfNulls<PartiQLValueType?>(N)
            for (type in edges) {
                arr[type.first] = type.second
            }
            return arr
        }

        /**
         * This table defines the rules in the SQL-99 section 9.3 BUT we don't have type constraints yet.
         */
        @JvmStatic
        internal val graph: SuperGraph = run {
            val graph = arrayOfNulls<Array<PartiQLValueType?>>(N)
            for (type in PartiQLValueType.values()) {
                // initialize all with empty edges
                graph[type] = arrayOfNulls(N)
            }
            graph[ANY] = edges()
            graph[NULL] = edges()
            graph[MISSING] = edges()
            graph[BOOL] = edges(
                BOOL to BOOL
            )
            graph[INT8] = edges(
                INT8 to INT8,
                INT16 to INT16,
                INT32 to INT32,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[INT16] = edges(
                INT8 to INT16,
                INT16 to INT16,
                INT32 to INT32,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[INT32] = edges(
                INT8 to INT32,
                INT16 to INT32,
                INT32 to INT32,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[INT64] = edges(
                INT8 to INT64,
                INT16 to INT64,
                INT32 to INT64,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[INT] = edges(
                INT8 to INT,
                INT16 to INT,
                INT32 to INT,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[DECIMAL] = edges(
                INT8 to DECIMAL,
                INT16 to DECIMAL,
                INT32 to DECIMAL,
                INT to DECIMAL,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[DECIMAL_ARBITRARY] = edges(
                INT8 to DECIMAL_ARBITRARY,
                INT16 to DECIMAL_ARBITRARY,
                INT32 to DECIMAL_ARBITRARY,
                INT to DECIMAL_ARBITRARY,
                DECIMAL to DECIMAL_ARBITRARY,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[FLOAT32] = edges(
                INT8 to FLOAT32,
                INT16 to FLOAT32,
                INT32 to FLOAT32,
                INT to FLOAT32,
                DECIMAL to FLOAT32,
                DECIMAL_ARBITRARY to FLOAT32,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[FLOAT64] = edges(
                INT8 to FLOAT64,
                INT16 to FLOAT64,
                INT32 to FLOAT64,
                INT to FLOAT64,
                DECIMAL to FLOAT64,
                DECIMAL_ARBITRARY to FLOAT64,
                FLOAT32 to FLOAT64,
                FLOAT64 to FLOAT64,
            )
            graph[CHAR] = edges(
                CHAR to CHAR,
                STRING to STRING,
                SYMBOL to STRING,
                CLOB to CLOB,
            )
            graph[STRING] = edges(
                CHAR to CHAR,
                STRING to STRING,
                SYMBOL to STRING,
                CLOB to CLOB,
            )
            graph[SYMBOL] = edges(
                CHAR to CHAR,
                STRING to STRING,
                SYMBOL to STRING,
                CLOB to CLOB,
            )
            graph[BINARY] = edges(
                BINARY to BINARY,
            )
            graph[BYTE] = edges(
                BYTE to BYTE,
                BLOB to BLOB,
            )
            graph[BLOB] = edges(
                BYTE to BLOB,
                BLOB to BLOB,
            )
            graph[DATE] = edges(
                DATE to DATE,
            )
            graph[CLOB] = edges(
                CHAR to CHAR,
                STRING to STRING,
                SYMBOL to STRING,
                CLOB to CLOB,
            )
            graph[TIME] = edges(
                TIME to TIME,
            )
            graph[TIMESTAMP] = edges(
                TIMESTAMP to TIMESTAMP,
            )
            graph[INTERVAL] = edges(
                INTERVAL to INTERVAL,
            )
            graph[LIST] = edges(
                LIST to LIST,
                SEXP to SEXP,
                BAG to BAG,
            )
            graph[SEXP] = edges(
                LIST to SEXP,
                SEXP to SEXP,
                BAG to BAG,
            )
            graph[BAG] = edges(
                LIST to BAG,
                SEXP to BAG,
                BAG to BAG,
            )
            graph[STRUCT] = edges(
                STRUCT to STRUCT,
            )
            graph.requireNoNulls()
        }
    }
}
