package org.partiql.planner.typer

import org.partiql.types.PartiQLValueType
import org.partiql.types.PartiQLValueType.BAG
import org.partiql.types.PartiQLValueType.BINARY
import org.partiql.types.PartiQLValueType.BLOB
import org.partiql.types.PartiQLValueType.BOOL
import org.partiql.types.PartiQLValueType.BYTE
import org.partiql.types.PartiQLValueType.CHAR
import org.partiql.types.PartiQLValueType.CLOB
import org.partiql.types.PartiQLValueType.DATE
import org.partiql.types.PartiQLValueType.DECIMAL
import org.partiql.types.PartiQLValueType.FLOAT32
import org.partiql.types.PartiQLValueType.FLOAT64
import org.partiql.types.PartiQLValueType.INT
import org.partiql.types.PartiQLValueType.INT16
import org.partiql.types.PartiQLValueType.INT32
import org.partiql.types.PartiQLValueType.INT64
import org.partiql.types.PartiQLValueType.INT8
import org.partiql.types.PartiQLValueType.INTERVAL
import org.partiql.types.PartiQLValueType.LIST
import org.partiql.types.PartiQLValueType.MISSING
import org.partiql.types.PartiQLValueType.NULL
import org.partiql.types.PartiQLValueType.NULLABLE_BAG
import org.partiql.types.PartiQLValueType.NULLABLE_BINARY
import org.partiql.types.PartiQLValueType.NULLABLE_BLOB
import org.partiql.types.PartiQLValueType.NULLABLE_BOOL
import org.partiql.types.PartiQLValueType.NULLABLE_BYTE
import org.partiql.types.PartiQLValueType.NULLABLE_CHAR
import org.partiql.types.PartiQLValueType.NULLABLE_CLOB
import org.partiql.types.PartiQLValueType.NULLABLE_DATE
import org.partiql.types.PartiQLValueType.NULLABLE_DECIMAL
import org.partiql.types.PartiQLValueType.NULLABLE_FLOAT32
import org.partiql.types.PartiQLValueType.NULLABLE_FLOAT64
import org.partiql.types.PartiQLValueType.NULLABLE_INT
import org.partiql.types.PartiQLValueType.NULLABLE_INT16
import org.partiql.types.PartiQLValueType.NULLABLE_INT32
import org.partiql.types.PartiQLValueType.NULLABLE_INT64
import org.partiql.types.PartiQLValueType.NULLABLE_INT8
import org.partiql.types.PartiQLValueType.NULLABLE_INTERVAL
import org.partiql.types.PartiQLValueType.NULLABLE_LIST
import org.partiql.types.PartiQLValueType.NULLABLE_SEXP
import org.partiql.types.PartiQLValueType.NULLABLE_STRING
import org.partiql.types.PartiQLValueType.NULLABLE_STRUCT
import org.partiql.types.PartiQLValueType.NULLABLE_SYMBOL
import org.partiql.types.PartiQLValueType.NULLABLE_TIME
import org.partiql.types.PartiQLValueType.NULLABLE_TIMESTAMP
import org.partiql.types.PartiQLValueType.SEXP
import org.partiql.types.PartiQLValueType.STRING
import org.partiql.types.PartiQLValueType.STRUCT
import org.partiql.types.PartiQLValueType.SYMBOL
import org.partiql.types.PartiQLValueType.TIME
import org.partiql.types.PartiQLValueType.TIMESTAMP

/**
 * Going with a matrix here (using enum ordinals) as it's simple and avoids walking.
 */
private typealias TypeGraph = Array<Array<Relationship?>>

/**
 * Each edge represents a type relationship
 */
private class Relationship(val castSafety: CastSafety)

/**
 * A CAST is safe iff it's lossless and never errs.
 *
 * SAFE     <-> IMPLICIT CAST
 * UNSAFE   <-> EXPLICIT CAST
 */
private enum class CastSafety { SAFE, UNSAFE }

/**
 * A place to model type relationships (for now this is to answer CAST inquiries).
 *
 * Is this indeed a lattice? It's a rather smart sounding word.
 */
internal class TypeLattice private constructor(
    private val types: Array<PartiQLValueType>,
    private val graph: TypeGraph,
) {

    /**
     * Returns a list of all implicit (safe) CAST pairs.
     */
    public fun implicitCasts(): List<Pair<PartiQLValueType, PartiQLValueType>> {
        val casts = mutableListOf<Pair<PartiQLValueType, PartiQLValueType>>()
        for (t1 in types) {
            for (t2 in types) {
                val r = graph[t1][t2]
                if (r != null && r.castSafety == CastSafety.SAFE) {
                    casts.add(t1 to t2)
                }
            }
        }
        return casts
    }

    /**
     * Dump the graph as an Asciidoc table.
     */
    override fun toString(): String = buildString {
        appendLine("|===")
        appendLine()
        // Header
        append("| | ").appendLine(types.joinToString("| "))
        // Body
        for (t1 in types) {
            append("| $t1 ")
            for (t2 in types) {
                val symbol = when (val r = graph[t1][t2]) {
                    null -> "-"
                    else -> when (r.castSafety) {
                        CastSafety.SAFE -> "⬤"
                        CastSafety.UNSAFE -> "✕"
                    }
                }
                append("| $symbol ")
            }
            appendLine()
        }
        appendLine()
        appendLine("|===")
    }

    private operator fun <T> Array<T>.get(t: PartiQLValueType): T = get(t.ordinal)

    companion object {

        private val N = PartiQLValueType.values().size

        private fun relationships(vararg relationships: Pair<PartiQLValueType, Relationship>): Array<Relationship?> {
            val arr = arrayOfNulls<Relationship?>(N)
            for (type in relationships) {
                arr[type.first] = type.second
            }
            return arr
        }

        private fun safe(): Relationship = Relationship(CastSafety.SAFE)

        private fun unsafe(): Relationship = Relationship(CastSafety.UNSAFE)

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)

        /**
         * Build the PartiQL type lattice.
         *
         * TODO define the unsafe / lossy / explicit CAST relationships.
         */
        public fun partiql(): TypeLattice {
            val types = PartiQLValueType.values()
            val graph = arrayOfNulls<Array<Relationship?>>(N)
            for (type in types) {
                // initialize all with empty relationships
                graph[type] = arrayOfNulls(N)
            }
            graph[NULL] = arrayOfNulls(N)
            graph[MISSING] = arrayOfNulls(N)
            graph[BOOL] = relationships(
                BOOL to safe(),
                INT8 to safe(),
                INT16 to safe(),
                INT32 to safe(),
                INT64 to safe(),
                INT to safe(),
                DECIMAL to safe(),
                FLOAT32 to safe(),
                FLOAT64 to safe(),
                CHAR to safe(),
                STRING to safe(),
                SYMBOL to safe(),
                NULLABLE_CHAR to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_INT8 to safe(),
                NULLABLE_INT16 to safe(),
                NULLABLE_INT32 to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
                NULLABLE_STRING to safe(),
                NULLABLE_SYMBOL to safe(),
            )
            graph[INT8] = relationships(
                BOOL to safe(),
                INT8 to safe(),
                INT16 to safe(),
                INT32 to safe(),
                INT64 to safe(),
                INT to safe(),
                DECIMAL to safe(),
                FLOAT32 to safe(),
                FLOAT64 to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_INT8 to safe(),
                NULLABLE_INT16 to safe(),
                NULLABLE_INT32 to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[INT16] = relationships(
                BOOL to safe(),
                INT16 to safe(),
                INT32 to safe(),
                INT64 to safe(),
                INT to safe(),
                DECIMAL to safe(),
                FLOAT32 to safe(),
                FLOAT64 to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_INT16 to safe(),
                NULLABLE_INT32 to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[INT32] = relationships(
                BOOL to safe(),
                INT32 to safe(),
                INT64 to safe(),
                INT to safe(),
                DECIMAL to safe(),
                FLOAT32 to safe(),
                FLOAT64 to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_INT32 to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[INT64] = relationships(
                BOOL to safe(),
                INT64 to safe(),
                INT to safe(),
                DECIMAL to safe(),
                FLOAT32 to safe(),
                FLOAT64 to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[INT] = relationships(
                BOOL to safe(),
                INT to safe(),
                DECIMAL to safe(),
                FLOAT32 to safe(),
                FLOAT64 to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[DECIMAL] = relationships(
                BOOL to safe(),
                DECIMAL to safe(),
                FLOAT32 to safe(),
                FLOAT64 to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[FLOAT32] = relationships(
                BOOL to safe(),
                FLOAT32 to safe(),
                FLOAT64 to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[FLOAT64] = relationships(
                BOOL to safe(),
                FLOAT64 to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[CHAR] = relationships(
                BOOL to safe(),
                CHAR to safe(),
                STRING to safe(),
                SYMBOL to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_CHAR to safe(),
                NULLABLE_STRING to safe(),
                NULLABLE_SYMBOL to safe(),
            )
            graph[STRING] = relationships(
                BOOL to safe(),
                STRING to safe(),
                SYMBOL to safe(),
                CLOB to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_STRING to safe(),
                NULLABLE_SYMBOL to safe(),
                NULLABLE_CLOB to safe(),
            )
            graph[SYMBOL] = relationships(
                BOOL to safe(),
                STRING to safe(),
                SYMBOL to safe(),
                CLOB to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_STRING to safe(),
                NULLABLE_SYMBOL to safe(),
                NULLABLE_CLOB to safe(),
            )
            graph[CLOB] = relationships(
                CLOB to safe(),
                NULLABLE_CLOB to safe(),
            )
            graph[BINARY] = arrayOfNulls(N)
            graph[BYTE] = arrayOfNulls(N)
            graph[BLOB] = arrayOfNulls(N)
            graph[DATE] = arrayOfNulls(N)
            graph[TIME] = arrayOfNulls(N)
            graph[TIMESTAMP] = arrayOfNulls(N)
            graph[INTERVAL] = arrayOfNulls(N)
            graph[BAG] = relationships(
                BAG to safe(),
                NULLABLE_BAG to safe(),
            )
            graph[LIST] = relationships(
                BAG to safe(),
                SEXP to safe(),
                LIST to safe(),
                NULLABLE_BAG to safe(),
                NULLABLE_SEXP to safe(),
                NULLABLE_LIST to safe(),
            )
            graph[SEXP] = relationships(
                BAG to safe(),
                SEXP to safe(),
                LIST to safe(),
                NULLABLE_BAG to safe(),
                NULLABLE_SEXP to safe(),
                NULLABLE_LIST to safe(),
            )
            graph[STRUCT] = relationships(
                STRUCT to safe(),
                NULLABLE_STRUCT to safe(),
            )
            graph[NULLABLE_BOOL] = relationships(
                NULLABLE_CHAR to safe(),
                NULLABLE_BOOL to safe(),
                NULLABLE_INT8 to safe(),
                NULLABLE_INT16 to safe(),
                NULLABLE_INT32 to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
                NULLABLE_STRING to safe(),
                NULLABLE_SYMBOL to safe(),
            )
            graph[NULLABLE_INT8] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_INT8 to safe(),
                NULLABLE_INT16 to safe(),
                NULLABLE_INT32 to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[NULLABLE_INT16] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_INT16 to safe(),
                NULLABLE_INT32 to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[NULLABLE_INT32] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_INT32 to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[NULLABLE_INT64] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_INT64 to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[NULLABLE_INT] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_INT to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[NULLABLE_DECIMAL] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_DECIMAL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[NULLABLE_FLOAT32] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_FLOAT32 to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[NULLABLE_FLOAT64] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_FLOAT64 to safe(),
            )
            graph[NULLABLE_CHAR] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_CHAR to safe(),
                NULLABLE_STRING to safe(),
                NULLABLE_SYMBOL to safe(),
            )
            graph[NULLABLE_STRING] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_STRING to safe(),
                NULLABLE_SYMBOL to safe(),
                NULLABLE_CLOB to safe(),
            )
            graph[NULLABLE_SYMBOL] = relationships(
                NULLABLE_BOOL to safe(),
                NULLABLE_STRING to safe(),
                NULLABLE_SYMBOL to safe(),
                NULLABLE_CLOB to safe(),
            )
            graph[NULLABLE_CLOB] = relationships(
                NULLABLE_CLOB to safe(),
            )
            graph[NULLABLE_BINARY] = arrayOfNulls(N)
            graph[NULLABLE_BYTE] = arrayOfNulls(N)
            graph[NULLABLE_BLOB] = arrayOfNulls(N)
            graph[NULLABLE_DATE] = arrayOfNulls(N)
            graph[NULLABLE_TIME] = arrayOfNulls(N)
            graph[NULLABLE_TIMESTAMP] = arrayOfNulls(N)
            graph[NULLABLE_INTERVAL] = arrayOfNulls(N)
            graph[NULLABLE_BAG] = relationships(
                NULLABLE_BAG to safe(),
            )
            graph[NULLABLE_LIST] = relationships(
                NULLABLE_BAG to safe(),
                NULLABLE_SEXP to safe(),
                NULLABLE_LIST to safe(),
            )
            graph[NULLABLE_SEXP] = relationships(
                NULLABLE_BAG to safe(),
                NULLABLE_SEXP to safe(),
                NULLABLE_LIST to safe(),
            )
            graph[NULLABLE_STRUCT] = relationships(
                NULLABLE_STRUCT to safe(),
            )
            return TypeLattice(types, graph.requireNoNulls())
        }
    }
}
