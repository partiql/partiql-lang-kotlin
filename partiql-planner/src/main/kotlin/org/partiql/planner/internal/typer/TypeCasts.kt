package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Cast
import org.partiql.planner.internal.ir.cast
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

@OptIn(PartiQLValueExperimental::class)
internal fun TypeRelationship.toCast(): Cast {
    val operand = this.castFn.parameters.first().type
    val target = this.castFn.returns
    return when (this.castType) {
        CastType.COERCION -> cast(operand, target, Cast.CastType.COERCION)
        CastType.EXPLICIT -> cast(operand, target, Cast.CastType.EXPLICIT)
        CastType.UNSAFE -> cast(operand, target, Cast.CastType.UNSAFE)
    }
}

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
    val types: Array<PartiQLValueType>,
    val graph: TypeGraph,
) {

    fun relationships(): Sequence<TypeRelationship> = sequence {
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

        private fun PartiQLValueType.relationships(coercion: List<PartiQLValueType>, explicit: List<PartiQLValueType>, unsafe: List<PartiQLValueType>): Array<TypeRelationship?> {
            return with(RelationshipBuilder(this)) {
                coercion.forEach { coercion(it) }
                explicit.forEach { explicit(it) }
                unsafe.forEach { unsafe(it) }
                build()
            }
        }

        private fun PartiQLValueType.selfCoercion(): Array<TypeRelationship?> {
            return with(RelationshipBuilder(this)) {
                coercion(this@selfCoercion)
                build()
            }
        }

        /**
         * Build the PartiQL type lattice.
         *
         * graph[operand] = [operand].relationship {
         *    func(target)
         * }
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
            // cast($any as type)
            // permit coercion when type is ANY
            // unsafe for all other types
            graph[ANY] = ANY.relationships(
                listOf(ANY),
                emptyList(),
                PartiQLValueType.values().filterNot { it == ANY }
            )
            // cast($null as type)
            // permits coercion when for any type
            graph[NULL] = NULL.relationships(
                PartiQLValueType.values().filterNot { it == MISSING },
                emptyList(),
                emptyList()
            )
            // cast($missing as type)
            // This does not make sense since function should propagate missing.
            graph[MISSING] = arrayOfNulls(N)
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
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                unsafe(INT64)
                explicit(INT)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                coercion(FLOAT32)
                coercion(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[FLOAT64] = FLOAT64.relationships {
                explicit(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                unsafe(INT64)
                explicit(INT)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                coercion(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[DECIMAL_ARBITRARY] = DECIMAL_ARBITRARY.relationships {
                explicit(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                unsafe(INT64)
                explicit(INT)
                explicit(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                explicit(FLOAT32)
                explicit(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[CHAR] = CHAR.relationships {
                unsafe(BOOL)
                coercion(CHAR)
                coercion(STRING)
                coercion(SYMBOL)
            }
            graph[STRING] = STRING.relationships {
                unsafe(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                unsafe(INT64)
                unsafe(INT)
                unsafe(DECIMAL)
                unsafe(DECIMAL_ARBITRARY)
                unsafe(FLOAT32)
                unsafe(FLOAT64)
                coercion(STRING)
                explicit(SYMBOL)
                coercion(CLOB)
            }
            graph[SYMBOL] = SYMBOL.relationships {
                unsafe(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                unsafe(INT64)
                unsafe(INT)
                unsafe(DECIMAL)
                unsafe(DECIMAL_ARBITRARY)
                unsafe(FLOAT32)
                unsafe(FLOAT64)
                coercion(STRING)
                coercion(SYMBOL)
                coercion(CLOB)
            }
            graph[CLOB] = CLOB.relationships {
                unsafe(STRING)
                unsafe(SYMBOL)
                unsafe(CHAR)
                coercion(CLOB)
            }
            graph[BINARY] = BINARY.selfCoercion()
            graph[BYTE] = BYTE.selfCoercion()
            graph[BLOB] = BLOB.selfCoercion()
            graph[DATE] = DATE.relationships {
                explicit(STRING)
                explicit(CHAR)
                explicit(SYMBOL)
                coercion(DATE)
                explicit(TIMESTAMP)
            }
            graph[TIME] = TIME.relationships {
                explicit(STRING)
                explicit(CHAR)
                explicit(SYMBOL)
                coercion(TIME)
                explicit(TIMESTAMP)
            }
            graph[TIMESTAMP] = TIMESTAMP.relationships {
                explicit(STRING)
                explicit(CHAR)
                explicit(SYMBOL)
                coercion(TIMESTAMP)
                explicit(DATE)
                explicit(TIME)
            }
            graph[INTERVAL] = INTERVAL.selfCoercion()
            graph[BAG] = BAG.relationships {
                coercion(BAG)
                explicit(SEXP)
                explicit(LIST)
            }
            graph[LIST] = LIST.relationships {
                explicit(BAG)
                explicit(SEXP)
                coercion(LIST)
            }
            graph[SEXP] = SEXP.relationships {
                explicit(BAG)
                coercion(SEXP)
                explicit(LIST)
            }
            graph[STRUCT] = STRUCT.selfCoercion()
            return TypeCasts(types, graph.requireNoNulls())
        }
    }

    private class RelationshipBuilder(val operand: PartiQLValueType) {

        private val relationships = arrayOfNulls<TypeRelationship?>(N)

        fun build() = relationships

        fun coercion(target: PartiQLValueType) {
            relationships[target] = TypeRelationship(CastType.COERCION, cast(operand, target))
        }

        fun explicit(target: PartiQLValueType) {
            relationships[target] = TypeRelationship(CastType.EXPLICIT, cast(operand, target))
        }

        fun unsafe(target: PartiQLValueType) {
            relationships[target] = TypeRelationship(CastType.UNSAFE, cast(operand, target))
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
}
