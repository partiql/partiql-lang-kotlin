package org.partiql.planner.internal.casts

import org.partiql.planner.internal.ir.Ref.Cast
import org.partiql.planner.internal.ir.refCast
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
 * A place to model type relationships (for now this is to answer CAST inquiries).
 *
 * @property types
 * @property graph      Going with a matrix here (using enum ordinals) as it's simple and avoids walking.
 */
@OptIn(PartiQLValueExperimental::class)
internal class CastTable private constructor(
    private val types: Array<PartiQLValueType>,
    private val graph: Array<Array<Cast?>>,
) {

    private fun relationships(): Sequence<Cast> = sequence {
        for (t1 in types) {
            for (t2 in types) {
                val r = graph[t1][t2]
                if (r != null) {
                    yield(r)
                }
            }
        }
    }

    fun get(operand: PartiQLValueType, target: PartiQLValueType): Cast? {
        val i = operand.ordinal
        val j = target.ordinal
        return graph[i][j]
    }

    /**
     * Returns the CAST function if exists, else null.
     */
    fun lookupCoercion(operand: PartiQLValueType, target: PartiQLValueType): Cast? {
        val i = operand.ordinal
        val j = target.ordinal
        val cast = graph[i][j] ?: return null
        return if (cast.safety == Cast.Safety.COERCION) cast else null
    }

    private operator fun <T> Array<T>.get(t: PartiQLValueType): T = get(t.ordinal)

    companion object {

        private val N = PartiQLValueType.values().size

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)

        private fun PartiQLValueType.relationships(block: RelationshipBuilder.() -> Unit): Array<Cast?> {
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
        @JvmStatic
        val partiql: CastTable = run {
            val types = PartiQLValueType.values()
            val graph = arrayOfNulls<Array<Cast?>>(N)
            for (type in types) {
                // initialize all with empty relationships
                graph[type] = arrayOfNulls(N)
            }
            graph[ANY] = ANY.relationships {
                coercion(ANY)
                PartiQLValueType.values().filterNot { it == ANY }.forEach {
                    unsafe(it)
                }
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
                explicit(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                unsafe(INT64)
                unsafe(INT)
                coercion(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                explicit(FLOAT32)
                explicit(FLOAT64)
                explicit(STRING)
                explicit(SYMBOL)
            }
            graph[DECIMAL_ARBITRARY] = DECIMAL_ARBITRARY.relationships {
                explicit(BOOL)
                unsafe(INT8)
                unsafe(INT16)
                unsafe(INT32)
                unsafe(INT64)
                unsafe(INT)
                coercion(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
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
                unsafe(INT)
                unsafe(DECIMAL)
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
                unsafe(INT)
                unsafe(DECIMAL)
                coercion(DECIMAL_ARBITRARY)
                unsafe(FLOAT32)
                coercion(FLOAT64)
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
            CastTable(types, graph.requireNoNulls())
        }
    }

    private class RelationshipBuilder(val operand: PartiQLValueType) {

        private val relationships = arrayOfNulls<Cast?>(N)

        fun build() = relationships

        fun coercion(target: PartiQLValueType) {
            relationships[target] = refCast(operand, target, Cast.Safety.COERCION)
        }

        fun explicit(target: PartiQLValueType) {
            relationships[target] = refCast(operand, target, Cast.Safety.EXPLICIT)
        }

        fun unsafe(target: PartiQLValueType) {
            relationships[target] = refCast(operand, target, Cast.Safety.UNSAFE)
        }
    }
}
