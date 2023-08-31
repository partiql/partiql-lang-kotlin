package org.partiql.planner.typer

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
 * Going with a matrix here (using enum ordinals) as it's simple and avoids walking.
 */
private typealias TypeGraph = Array<Array<Relationship?>>

/**
 * Each edge represents a type relationship
 */
private data class Relationship(val type: CastType)

/**
 * An COERCION will be inserted by the compiler during function resolution, an EXPLICIT CAST cannot be inserted.
 */
private enum class CastType { IMPLICIT, EXPLICIT_LOSSLESS, EXPLICIT_LOSSY }

/**
 * A place to model type relationships (for now this is to answer CAST inquiries).
 *
 * Is this indeed a lattice? It's a rather smart sounding word.
 */
@OptIn(PartiQLValueExperimental::class)
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
                    null -> "X"
                    else -> when (r.type) {
                        CastType.IMPLICIT -> "⬤"
                        CastType.EXPLICIT_LOSSLESS -> "◯"
                        CastType.EXPLICIT_LOSSY -> "△"
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
            graph[ANY] = relationships(
                ANY to implicit()
            )
            graph[NULL] = relationships(
                NULL to implicit()
            )
            graph[MISSING] = relationships(
                MISSING to implicit()
            )
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
            )
            graph[INT] = relationships(
                BOOL to lossless(),
                INT to implicit(),
                DECIMAL to implicit(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
            )
            graph[DECIMAL] = relationships(
                BOOL to lossless(),
                DECIMAL to implicit(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
            )
            graph[FLOAT32] = relationships(
                BOOL to lossless(),
                FLOAT32 to implicit(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
            )
            graph[FLOAT64] = relationships(
                BOOL to lossless(),
                FLOAT64 to implicit(),
                STRING to lossless(),
                SYMBOL to lossless(),
            )
            graph[CHAR] = relationships(
                BOOL to lossless(),
                CHAR to implicit(),
                STRING to implicit(),
                SYMBOL to implicit(),
            )
            graph[STRING] = relationships(
                BOOL to lossless(),
                STRING to implicit(),
                SYMBOL to implicit(),
                CLOB to implicit(),
            )
            graph[SYMBOL] = relationships(
                BOOL to lossless(),
                STRING to implicit(),
                SYMBOL to implicit(),
                CLOB to implicit(),
            )
            graph[CLOB] = relationships(
                CLOB to implicit(),
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
            )
            graph[LIST] = relationships(
                BAG to implicit(),
                SEXP to implicit(),
                LIST to implicit(),
            )
            graph[SEXP] = relationships(
                BAG to implicit(),
                SEXP to implicit(),
                LIST to implicit(),
            )
            graph[STRUCT] = relationships(
                STRUCT to implicit(),
            )
            return TypeLattice(types, graph.requireNoNulls())
        }
    }
}
