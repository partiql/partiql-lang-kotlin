package org.partiql.planner.internal.casts

import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Ref.Cast
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.types.PType
import org.partiql.types.PType.Kind
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * A place to model type relationships (for now this is to answer CAST inquiries).
 *
 * @property types
 * @property graph      Going with a matrix here (using enum ordinals) as it's simple and avoids walking.
 */
@OptIn(PartiQLValueExperimental::class)
internal class CastTable private constructor(
    private val types: Array<Kind>,
    private val graph: Array<Array<Status>>,
) {

    fun get(operand: PType, target: PType): Cast? {
        val i = operand.kind.ordinal
        val j = target.kind.ordinal
        return when (graph[i][j]) {
            Status.YES, Status.MODIFIED -> Cast(CompilerType(operand), CompilerType(target), Ref.Cast.Safety.COERCION, isNullable = true)
            Status.NO -> null
        }
    }

    private operator fun <T> Array<T>.get(t: PartiQLValueType): T = get(t.ordinal)

    /**
     * This represents the Y, M, and N in the table listed in SQL:1999 Section 6.22.
     */
    internal enum class Status {
        YES,
        NO,
        MODIFIED
    }

    companion object {

        private val N = Kind.values().size

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)

        private fun relationships(block: RelationshipBuilder.() -> Unit): Array<Status> {
            return with(RelationshipBuilder()) {
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
            val types = Kind.values()
            val graph = arrayOfNulls<Array<Status>>(N)
            for (type in types) {
                // initialize all with empty relationships
                graph[type.ordinal] = Array(N) { Status.NO }
            }
            graph[Kind.DYNAMIC.ordinal] = relationships {
                cast(Kind.DYNAMIC)
                Kind.values().filterNot { it == Kind.DYNAMIC }.forEach {
                    cast(it)
                }
            }
            graph[Kind.BOOL.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.CHAR)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.TINYINT.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.SMALLINT.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.INTEGER.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.BIGINT.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.NUMERIC.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.DECIMAL.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.DECIMAL_ARBITRARY.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.REAL.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.DOUBLE.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.DECIMAL_ARBITRARY)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.CHAR.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.CHAR)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
            }
            graph[Kind.STRING.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
                cast(Kind.CLOB)
            }
            graph[Kind.VARCHAR.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
                cast(Kind.CLOB)
            }
            graph[Kind.SYMBOL.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.SYMBOL)
                cast(Kind.CLOB)
            }
            graph[Kind.CLOB.ordinal] = relationships {
                cast(Kind.CLOB)
            }
            graph[Kind.BLOB.ordinal] = Array(N) { Status.NO }
            graph[Kind.DATE.ordinal] = Array(N) { Status.NO }
            graph[Kind.TIMEZ.ordinal] = Array(N) { Status.NO }
            graph[Kind.TIME.ordinal] = Array(N) { Status.NO }
            graph[Kind.TIMESTAMPZ.ordinal] = Array(N) { Status.NO }
            graph[Kind.TIMESTAMP.ordinal] = Array(N) { Status.NO }
            graph[Kind.BAG.ordinal] = relationships {
                cast(Kind.BAG)
            }
            graph[Kind.ARRAY.ordinal] = relationships {
                cast(Kind.BAG)
                cast(Kind.SEXP)
                cast(Kind.ARRAY)
            }
            graph[Kind.SEXP.ordinal] = relationships {
                cast(Kind.BAG)
                cast(Kind.SEXP)
                cast(Kind.ARRAY)
            }
            graph[Kind.STRUCT.ordinal] = relationships {
                cast(Kind.STRUCT)
            }
            CastTable(types, graph.requireNoNulls())
        }
    }

    /**
     * TODO: Add another method to support [Status.MODIFIED]. See the cast table at SQL:1999 Section 6.22
     */
    private class RelationshipBuilder {

        private val relationships = Array(N) { Status.NO }

        fun build() = relationships

        fun cast(target: Kind) {
            relationships[target.ordinal] = Status.YES
        }
    }
}
