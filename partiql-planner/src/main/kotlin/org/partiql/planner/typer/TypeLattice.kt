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
private data class Relationship(val type: CastType)

/**
 * An IMPLICIT CAST will be inserted by the compiler during function resolution, an EXPLICIT CAST cannot be inserted.
 */
private enum class CastType { IMPLICIT, EXPLICIT_LOSSLESS, EXPLICIT_LOSSY }

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
     * Returns a list of all implicit CAST pairs.
     */
    public fun implicitCasts(): List<Pair<PartiQLValueType, PartiQLValueType>> {
        val casts = mutableListOf<Pair<PartiQLValueType, PartiQLValueType>>()
        for (t1 in types) {
            for (t2 in types) {
                val r = graph[t1][t2]
                if (r != null && r.type == CastType.IMPLICIT) {
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
                    else -> r.toString()
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

        private fun implicit(): Relationship = Relationship(CastType.IMPLICIT)

        private fun lossless(): Relationship = Relationship(CastType.EXPLICIT_LOSSLESS)

        private fun lossy(): Relationship = Relationship(CastType.EXPLICIT_LOSSY)

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)

        /**
         * Build the PartiQL type lattice.
         *
         * TODO this is incomplete.
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
                BOOL to implicit(),
                INT8 to implicit(),
                INT16 to implicit(),
                INT32 to implicit(),
                INT64 to implicit(),
                INT to implicit(),
                DECIMAL to implicit(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                CHAR to implicit(),
                STRING to implicit(),
                SYMBOL to implicit(),
                NULLABLE_CHAR to implicit(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT8 to implicit(),
                NULLABLE_INT16 to implicit(),
                NULLABLE_INT32 to implicit(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to implicit(),
                NULLABLE_SYMBOL to implicit(),
            )
            graph[INT8] = relationships(
                BOOL to lossless(),
                INT8 to implicit(),
                INT16 to implicit(),
                INT32 to implicit(),
                INT64 to implicit(),
                INT to implicit(),
                DECIMAL to implicit(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT8 to implicit(),
                NULLABLE_INT16 to implicit(),
                NULLABLE_INT32 to implicit(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[INT16] = relationships(
                BOOL to lossless(),
                INT16 to implicit(),
                INT32 to implicit(),
                INT64 to implicit(),
                INT to implicit(),
                DECIMAL to implicit(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT16 to implicit(),
                NULLABLE_INT32 to implicit(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[INT32] = relationships(
                BOOL to lossless(),
                INT32 to implicit(),
                INT64 to implicit(),
                INT to implicit(),
                DECIMAL to implicit(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT32 to implicit(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[INT64] = relationships(
                BOOL to lossless(),
                INT64 to implicit(),
                INT to implicit(),
                DECIMAL to implicit(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[INT] = relationships(
                BOOL to lossless(),
                INT to implicit(),
                DECIMAL to implicit(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[DECIMAL] = relationships(
                BOOL to lossless(),
                DECIMAL to implicit(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[FLOAT32] = relationships(
                BOOL to lossless(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[FLOAT64] = relationships(
                BOOL to lossless(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[CHAR] = relationships(
                BOOL to lossless(),
                CHAR to implicit(),
                STRING to implicit(),
                SYMBOL to implicit(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_CHAR to implicit(),
                NULLABLE_STRING to implicit(),
                NULLABLE_SYMBOL to implicit(),
            )
            graph[STRING] = relationships(
                BOOL to lossless(),
                STRING to implicit(),
                SYMBOL to implicit(),
                CLOB to implicit(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_STRING to implicit(),
                NULLABLE_SYMBOL to implicit(),
                NULLABLE_CLOB to implicit(),
            )
            graph[SYMBOL] = relationships(
                BOOL to lossless(),
                STRING to implicit(),
                SYMBOL to implicit(),
                CLOB to implicit(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_STRING to implicit(),
                NULLABLE_SYMBOL to implicit(),
                NULLABLE_CLOB to implicit(),
            )
            graph[CLOB] = relationships(
                CLOB to implicit(),
                NULLABLE_CLOB to implicit(),
            )
            graph[BINARY] = arrayOfNulls(N)
            graph[BYTE] = arrayOfNulls(N)
            graph[BLOB] = arrayOfNulls(N)
            graph[DATE] = arrayOfNulls(N)
            graph[TIME] = arrayOfNulls(N)
            graph[TIMESTAMP] = arrayOfNulls(N)
            graph[INTERVAL] = arrayOfNulls(N)
            graph[BAG] = relationships(
                BAG to implicit(),
                NULLABLE_BAG to implicit(),
            )
            graph[LIST] = relationships(
                BAG to implicit(),
                SEXP to implicit(),
                LIST to implicit(),
                NULLABLE_BAG to implicit(),
                NULLABLE_SEXP to implicit(),
                NULLABLE_LIST to implicit(),
            )
            graph[SEXP] = relationships(
                BAG to implicit(),
                SEXP to implicit(),
                LIST to implicit(),
                NULLABLE_BAG to implicit(),
                NULLABLE_SEXP to implicit(),
                NULLABLE_LIST to implicit(),
            )
            graph[STRUCT] = relationships(
                STRUCT to implicit(),
                NULLABLE_STRUCT to implicit(),
            )
            graph[NULLABLE_BOOL] = relationships(
                NULLABLE_CHAR to implicit(),
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT8 to implicit(),
                NULLABLE_INT16 to implicit(),
                NULLABLE_INT32 to implicit(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to implicit(),
                NULLABLE_SYMBOL to implicit(),
            )
            graph[NULLABLE_INT8] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT8 to implicit(),
                NULLABLE_INT16 to implicit(),
                NULLABLE_INT32 to implicit(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[NULLABLE_INT16] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT16 to implicit(),
                NULLABLE_INT32 to implicit(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[NULLABLE_INT32] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT32 to implicit(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[NULLABLE_INT64] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT64 to implicit(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[NULLABLE_INT] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_INT to implicit(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[NULLABLE_DECIMAL] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_DECIMAL to implicit(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[NULLABLE_FLOAT32] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_FLOAT32 to implicit(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[NULLABLE_FLOAT64] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_FLOAT64 to implicit(),
                NULLABLE_STRING to lossless(),
                NULLABLE_SYMBOL to lossless(),
            )
            graph[NULLABLE_CHAR] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_CHAR to implicit(),
                NULLABLE_STRING to implicit(),
                NULLABLE_SYMBOL to implicit(),
            )
            graph[NULLABLE_STRING] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_STRING to implicit(),
                NULLABLE_SYMBOL to implicit(),
                NULLABLE_CLOB to implicit(),
            )
            graph[NULLABLE_SYMBOL] = relationships(
                NULLABLE_BOOL to lossless(),
                NULLABLE_STRING to implicit(),
                NULLABLE_SYMBOL to implicit(),
                NULLABLE_CLOB to implicit(),
            )
            graph[NULLABLE_CLOB] = relationships(
                NULLABLE_CLOB to implicit(),
            )
            graph[NULLABLE_BINARY] = arrayOfNulls(N)
            graph[NULLABLE_BYTE] = arrayOfNulls(N)
            graph[NULLABLE_BLOB] = arrayOfNulls(N)
            graph[NULLABLE_DATE] = arrayOfNulls(N)
            graph[NULLABLE_TIME] = arrayOfNulls(N)
            graph[NULLABLE_TIMESTAMP] = arrayOfNulls(N)
            graph[NULLABLE_INTERVAL] = arrayOfNulls(N)
            graph[NULLABLE_BAG] = relationships(
                NULLABLE_BAG to implicit(),
            )
            graph[NULLABLE_LIST] = relationships(
                NULLABLE_BAG to implicit(),
                NULLABLE_SEXP to implicit(),
                NULLABLE_LIST to implicit(),
            )
            graph[NULLABLE_SEXP] = relationships(
                NULLABLE_BAG to implicit(),
                NULLABLE_SEXP to implicit(),
                NULLABLE_LIST to implicit(),
            )
            graph[NULLABLE_STRUCT] = relationships(
                NULLABLE_STRUCT to implicit(),
            )
            return TypeLattice(types, graph.requireNoNulls())
        }
    }
}
