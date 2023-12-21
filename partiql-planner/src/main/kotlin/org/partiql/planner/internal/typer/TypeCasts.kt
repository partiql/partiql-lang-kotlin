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
 * Going with a matrix here (using enum ordinals) as it's simple and avoids walking.
 */
internal typealias TypeGraph = Array<Array<TypeRelationship?>>

/**
 * Each edge represents a type relationship
 */
internal data class TypeRelationship(
    val castType: CastType,
    val castFn: FunctionSignature.Scalar,
)

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
 */
@OptIn(PartiQLValueExperimental::class)
internal class TypeCasts private constructor(
    public val types: Array<PartiQLValueType>,
    public val graph: TypeGraph,
) {

    internal fun relationships(): Sequence<TypeRelationship> = sequence {
        for (t1 in types) {
            for (t2 in types) {
                val r = graph[t1][t2]
                if (r != null) {
                    yield(r)
                }
            }
        }
    }

    /**
     * Cache a list of unsafe cast SPECIFIC for easy typing lookup
     */
    val unsafeCastSet: Set<String> by lazy {
        val set = mutableSetOf<String>()
        relationships().forEach {
            if (it.castType == CastType.UNSAFE) {
                set.add(it.castFn.specific)
            }
        }
        set
    }

    private operator fun <T> Array<T>.get(t: PartiQLValueType): T = get(t.ordinal)

    companion object {

        private val N = PartiQLValueType.values().size

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)

        private fun PartiQLValueType.relationships(block: RelationshipBuilder.() -> Unit): Array<TypeRelationship?> {
            return with(RelationshipBuilder(this)) {
                block()
                build()
            }
        }

        /**
         * Build the PartiQL type lattice.
         *
         * TODO this is incomplete.
         */
        public fun partiql(): TypeCasts {
            val types = PartiQLValueType.values()
            val graph = arrayOfNulls<Array<TypeRelationship?>>(N)
            for (type in types) {
                // initialize all with empty relationships
                graph[type] = arrayOfNulls(N)
            }
            graph[ANY] = ANY.relationships {
                coercion(ANY)
            }
            graph[NULL] = NULL.relationships {
                coercion(NULL)
            }
            graph[MISSING] = MISSING.relationships {
                coercion(MISSING)
            }
            graph[BOOL] = BOOL.relationships {
                coercion(BOOL)
                explicit(INT8)
                explicit(INT16)
                explicit(INT32)
                explicit(INT64)
                explicit(INT)
                explicit(DECIMAL)
                explicit(DECIMAL_ARBITRARY)
                explicit(FLOAT32)
                explicit(FLOAT64)
                explicit(CHAR)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[INT8] = INT8.relationships {
                explicit(BOOL)
                coercion(INT8)
                coercion(INT16)
                coercion(INT32)
                coercion(INT64)
                coercion(INT)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                coercion(FLOAT32)
                coercion(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[INT16] = INT16.relationships {
                explicit(BOOL)
                unsafe(INT8)
                coercion(INT16)
                coercion(INT32)
                coercion(INT64)
                coercion(INT)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                coercion(FLOAT32)
                coercion(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[INT32] = INT32.relationships {
                explicit(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                coercion(INT32)
                coercion(INT64)
                coercion(INT)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                coercion(FLOAT32)
                coercion(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[INT64] = INT64.relationships {
                explicit(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                coercion(INT64)
                coercion(INT)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                coercion(FLOAT32)
                coercion(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[INT] = INT.relationships {
                explicit(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                unsafe(INT64)
                coercion(INT)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                coercion(FLOAT32)
                coercion(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[DECIMAL] = DECIMAL.relationships {
                explicit(INT8)
                explicit(INT16)
                explicit(INT32)
                explicit(INT64)
                explicit(BOOL)
                explicit(DECIMAL)
                explicit(DECIMAL_ARBITRARY)
                explicit(FLOAT32)
                explicit(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[FLOAT32] = FLOAT32.relationships {
                explicit(BOOL)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                coercion(FLOAT32)
                coercion(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[FLOAT64] = FLOAT64.relationships {
                explicit(BOOL)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                coercion(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[DECIMAL_ARBITRARY] = DECIMAL_ARBITRARY.relationships {
                explicit(BOOL)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                explicit(FLOAT32)
                explicit(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[CHAR] = CHAR.relationships {
                explicit(BOOL)
                coercion(CHAR)
                coercion(STRING)
                coercion(SYMBOL)
            }
            graph[STRING] = STRING.relationships {
                explicit(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                unsafe(INT64)
                unsafe(INT)
                coercion(STRING)
                explicit(SYMBOL)
                coercion(CLOB)
            }
            graph[SYMBOL] = SYMBOL.relationships {
                explicit(BOOL)
                coercion(STRING)
                coercion(SYMBOL)
                coercion(CLOB)
            }
            graph[CLOB] = CLOB.relationships {
                coercion(CLOB)
            }
            graph[BINARY] = arrayOfNulls(N)
            graph[BYTE] = arrayOfNulls(N)
            graph[BLOB] = arrayOfNulls(N)
            graph[DATE] = arrayOfNulls(N)
            graph[TIME] = arrayOfNulls(N)
            graph[TIMESTAMP] = arrayOfNulls(N)
            graph[INTERVAL] = arrayOfNulls(N)
            graph[BAG] = BAG.relationships {
                coercion(BAG)
            }
            graph[LIST] = LIST.relationships {
                coercion(BAG)
                coercion(SEXP)
                coercion(LIST)
            }
            graph[SEXP] = SEXP.relationships {
                coercion(BAG)
                coercion(SEXP)
                coercion(LIST)
            }
            graph[STRUCT] = STRUCT.relationships {
                coercion(STRUCT)
            }
            return TypeCasts(types, graph.requireNoNulls())
        }
    }

    private class RelationshipBuilder(val source: PartiQLValueType) {

        private val relationships = arrayOfNulls<TypeRelationship?>(N)

        fun build() = relationships

        fun coercion(target: PartiQLValueType) {
            relationships[target] = TypeRelationship(CastType.COERCION, cast(source, target))
        }

        fun explicit(target: PartiQLValueType) {
            relationships[target] = TypeRelationship(CastType.EXPLICIT, cast(source, target))
        }

        fun unsafe(target: PartiQLValueType) {
            relationships[target] = TypeRelationship(CastType.UNSAFE, cast(source, target))
        }

        private fun cast(operand: PartiQLValueType, target: PartiQLValueType) =
            FunctionSignature.Scalar(
                name = "cast_${target.name.lowercase()}",
                returns = target,
                parameters = listOf(
                    FunctionParameter("value", operand),
                ),
                isNullable = false,
                isNullCall = true
            )
    }

    // /**
    //  * Dump the graph as an Asciidoc table.
    //  */
    // override fun toString(): String = buildString {
    //     appendLine("|===")
    //     appendLine()
    //     // Header
    //     append("| | ").appendLine(types.joinToString("| "))
    //     // Body
    //     for (t1 in types) {
    //         append("| $t1 ")
    //         for (t2 in types) {
    //             val symbol = when (val r = graph[t1][t2]) {
    //                 null -> "X"
    //                 else -> when (r.castType) {
    //                     CastType.COERCION -> "⬤"
    //                     CastType.EXPLICIT -> "◯"
    //                     CastType.UNSAFE -> "△"
    //                 }
    //             }
    //             append("| $symbol ")
    //         }
    //         appendLine()
    //     }
    //     appendLine()
    //     appendLine("|===")
    // }
}
