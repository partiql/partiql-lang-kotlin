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
internal typealias TypeGraph = Array<Array<TypeRelationship?>>

/**
 * Each edge represents a type relationship
 */
internal data class TypeRelationship(val cast: CastType)

/**
 * An COERCION will be inserted by the compiler during function resolution, an EXPLICIT CAST will never be inserted.
 *
 * COERCION: Lossless CAST(V AS T) -> T
 * EXPLICIT: Lossy    CAST(V AS T) -> T
 * UNSAFE:            CAST(V AS T) -> T|MISSING
 */
internal enum class CastType { COERCION, EXPLICIT, UNSAFE }

/**
 * A place to model type relationships (for now this is to answer CAST inquiries).
 *
 * Is this indeed a lattice? It's a rather smart sounding word.
 */
@OptIn(PartiQLValueExperimental::class)
internal class TypeLattice private constructor(
    public val types: Array<PartiQLValueType>,
    public val graph: TypeGraph,
) {

    public fun canCoerce(operand: PartiQLValueType, target: PartiQLValueType): Boolean {
        return graph[operand][target]?.cast == CastType.COERCION
    }

    internal val all = PartiQLValueType.values()

    internal val nullable = listOf(
        NULL, // null.null
        MISSING, // missing
    )

    internal val integer = listOf(
        INT8,
        INT16,
        INT32,
        INT64,
        INT,
    )

    internal val numeric = listOf(
        INT8,
        INT16,
        INT32,
        INT64,
        INT,
        DECIMAL,
        FLOAT32,
        FLOAT64,
    )

    internal val text = listOf(
        STRING,
        SYMBOL,
        CLOB,
    )

    internal val collections = listOf(
        BAG,
        LIST,
        SEXP,
    )

    internal val datetime = listOf(
        DATE,
        TIME,
        TIMESTAMP,
    )

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
                    else -> when (r.cast) {
                        CastType.COERCION -> "⬤"
                        CastType.EXPLICIT -> "◯"
                        CastType.UNSAFE -> "△"
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

        private fun relationships(vararg relationships: Pair<PartiQLValueType, TypeRelationship>): Array<TypeRelationship?> {
            val arr = arrayOfNulls<TypeRelationship?>(N)
            for (type in relationships) {
                arr[type.first] = type.second
            }
            return arr
        }

        private fun coercion(): TypeRelationship = TypeRelationship(CastType.COERCION)

        private fun explicit(): TypeRelationship = TypeRelationship(CastType.EXPLICIT)

        private fun unsafe(): TypeRelationship = TypeRelationship(CastType.UNSAFE)

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)

        /**
         * Build the PartiQL type lattice.
         *
         * TODO this is incomplete.
         */
        public fun partiql(): TypeLattice {
            val types = PartiQLValueType.values()
            val graph = arrayOfNulls<Array<TypeRelationship?>>(N)
            for (type in types) {
                // initialize all with empty relationships
                graph[type] = arrayOfNulls(N)
            }
            graph[ANY] = relationships(
                ANY to coercion()
            )
            graph[NULL] = relationships(
                NULL to coercion()
            )
            graph[MISSING] = relationships(
                MISSING to coercion()
            )
            graph[BOOL] = relationships(
                BOOL to coercion(),
                INT8 to coercion(),
                INT16 to coercion(),
                INT32 to coercion(),
                INT64 to coercion(),
                INT to coercion(),
                DECIMAL to coercion(),
                FLOAT32 to coercion(),
                FLOAT64 to coercion(),
                CHAR to coercion(),
                STRING to coercion(),
                SYMBOL to coercion(),
            )
            graph[INT8] = relationships(
                BOOL to explicit(),
                INT8 to coercion(),
                INT16 to coercion(),
                INT32 to coercion(),
                INT64 to coercion(),
                INT to coercion(),
                DECIMAL to coercion(),
                FLOAT32 to coercion(),
                FLOAT64 to coercion(),
                STRING to explicit(),
                SYMBOL to explicit(),
            )
            graph[INT16] = relationships(
                BOOL to explicit(),
                INT8 to unsafe(),
                INT16 to coercion(),
                INT32 to coercion(),
                INT64 to coercion(),
                INT to coercion(),
                DECIMAL to coercion(),
                FLOAT32 to coercion(),
                FLOAT64 to coercion(),
                STRING to explicit(),
                SYMBOL to explicit(),
            )
            graph[INT32] = relationships(
                BOOL to explicit(),
                INT8 to unsafe(),
                INT16 to unsafe(),
                INT32 to coercion(),
                INT64 to coercion(),
                INT to coercion(),
                DECIMAL to coercion(),
                FLOAT32 to coercion(),
                FLOAT64 to coercion(),
                STRING to explicit(),
                SYMBOL to explicit(),
            )
            graph[INT64] = relationships(
                BOOL to explicit(),
                INT8 to unsafe(),
                INT16 to unsafe(),
                INT32 to unsafe(),
                INT64 to coercion(),
                INT to coercion(),
                DECIMAL to coercion(),
                FLOAT32 to coercion(),
                FLOAT64 to coercion(),
                STRING to explicit(),
                SYMBOL to explicit(),
            )
            graph[INT] = relationships(
                BOOL to explicit(),
                INT8 to unsafe(),
                INT16 to unsafe(),
                INT32 to unsafe(),
                INT64 to unsafe(),
                INT to coercion(),
                DECIMAL to coercion(),
                FLOAT32 to coercion(),
                FLOAT64 to coercion(),
                STRING to explicit(),
                SYMBOL to explicit(),
            )
            graph[DECIMAL] = relationships(
                BOOL to explicit(),
                DECIMAL to coercion(),
                FLOAT32 to coercion(),
                FLOAT64 to coercion(),
                STRING to explicit(),
                SYMBOL to explicit(),
            )
            graph[FLOAT32] = relationships(
                BOOL to explicit(),
                FLOAT32 to coercion(),
                FLOAT64 to coercion(),
                STRING to explicit(),
                SYMBOL to explicit(),
            )
            graph[FLOAT64] = relationships(
                BOOL to explicit(),
                FLOAT64 to coercion(),
                STRING to explicit(),
                SYMBOL to explicit(),
            )
            graph[CHAR] = relationships(
                BOOL to explicit(),
                CHAR to coercion(),
                STRING to coercion(),
                SYMBOL to coercion(),
            )
            graph[STRING] = relationships(
                BOOL to explicit(),
                INT8 to unsafe(),
                INT16 to unsafe(),
                INT32 to unsafe(),
                INT64 to unsafe(),
                INT to unsafe(),
                STRING to coercion(),
                SYMBOL to coercion(),
                CLOB to coercion(),
            )
            graph[SYMBOL] = relationships(
                BOOL to explicit(),
                STRING to coercion(),
                SYMBOL to coercion(),
                CLOB to coercion(),
            )
            graph[CLOB] = relationships(
                CLOB to coercion(),
            )
            graph[BINARY] = arrayOfNulls(N)
            graph[BYTE] = arrayOfNulls(N)
            graph[BLOB] = arrayOfNulls(N)
            graph[DATE] = arrayOfNulls(N)
            graph[TIME] = arrayOfNulls(N)
            graph[TIMESTAMP] = arrayOfNulls(N)
            graph[INTERVAL] = arrayOfNulls(N)
            graph[BAG] = relationships(
                BAG to coercion(),
            )
            graph[LIST] = relationships(
                BAG to coercion(),
                SEXP to coercion(),
                LIST to coercion(),
            )
            graph[SEXP] = relationships(
                BAG to coercion(),
                SEXP to coercion(),
                LIST to coercion(),
            )
            graph[STRUCT] = relationships(
                STRUCT to coercion(),
            )
            return TypeLattice(types, graph.requireNoNulls())
        }
    }
}
