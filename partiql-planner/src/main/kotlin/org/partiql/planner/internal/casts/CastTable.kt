package org.partiql.planner.internal.casts

import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Ref.Cast
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.types.PType
import org.partiql.types.PType.Kind

/**
 * A place to model type relationships (for now this is to answer CAST inquiries).
 *
 * @property types
 * @property graph      Going with a matrix here (using enum ordinals) as it's simple and avoids walking.
 */
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

    /**
     * This represents the Y, M, and N in the table listed in SQL:1999 Section 6.22.
     */
    internal enum class Status {
        YES,
        NO,
        MODIFIED
    }

    companion object {

        private val N = Kind.entries.size

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
            val types = Kind.entries.toTypedArray()
            val graph = arrayOfNulls<Array<Status>>(N)
            for (type in types) {
                // initialize all with empty relationships
                graph[type.ordinal] = Array(N) { Status.NO }
            }
            graph[Kind.UNKNOWN.ordinal] = relationships {
                PType.Kind.values().map {
                    cast(it)
                }
            }
            graph[Kind.DYNAMIC.ordinal] = relationships {
                cast(Kind.DYNAMIC)
                Kind.entries.filterNot { it == Kind.DYNAMIC }.forEach {
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
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.CHAR)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
            }
            graph[Kind.TINYINT.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.CHAR)
            }
            graph[Kind.SMALLINT.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.CHAR)
                cast(Kind.VARCHAR)
            }
            graph[Kind.INTEGER.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.CHAR)
                cast(Kind.VARCHAR)
            }
            graph[Kind.BIGINT.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.CHAR)
                cast(Kind.VARCHAR)
            }
            graph[Kind.NUMERIC.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.CHAR)
                cast(Kind.VARCHAR)
            }
            graph[Kind.DECIMAL.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.CHAR)
                cast(Kind.VARCHAR)
            }
            graph[Kind.REAL.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.CHAR)
                cast(Kind.VARCHAR)
            }
            graph[Kind.DOUBLE.ordinal] = relationships {
                cast(Kind.BOOL)
                cast(Kind.TINYINT)
                cast(Kind.SMALLINT)
                cast(Kind.INTEGER)
                cast(Kind.BIGINT)
                cast(Kind.NUMERIC)
                cast(Kind.DECIMAL)
                cast(Kind.REAL)
                cast(Kind.DOUBLE)
                cast(Kind.STRING)
                cast(Kind.CHAR)
                cast(Kind.VARCHAR)
            }
            graph[Kind.CHAR.ordinal] = relationships {
                Kind.values().filterNot {
                    it in setOf(Kind.BLOB, Kind.UNKNOWN, Kind.DYNAMIC, Kind.ARRAY, Kind.BAG, Kind.ROW)
                }.forEach {
                    cast(it)
                }
            }
            graph[Kind.STRING.ordinal] = relationships {
                Kind.values().filterNot {
                    it in setOf(Kind.BLOB, Kind.UNKNOWN, Kind.DYNAMIC, Kind.ARRAY, Kind.BAG, Kind.ROW)
                }.forEach {
                    cast(it)
                }
            }
            graph[Kind.VARCHAR.ordinal] = relationships {
                Kind.values().filterNot {
                    it in setOf(Kind.BLOB, Kind.UNKNOWN, Kind.DYNAMIC, Kind.ARRAY, Kind.BAG, Kind.ROW)
                }.forEach {
                    cast(it)
                }
            }
            graph[Kind.CLOB.ordinal] = relationships {
                Kind.values().filterNot {
                    it in setOf(Kind.BLOB, Kind.UNKNOWN, Kind.DYNAMIC, Kind.ARRAY, Kind.BAG, Kind.ROW)
                }.forEach {
                    cast(it)
                }
            }
            graph[Kind.BLOB.ordinal] = Array(N) { Status.NO }
            graph[Kind.DATE.ordinal] = relationships {
                cast(Kind.TIMESTAMPZ)
                cast(Kind.TIMESTAMP)
                cast(Kind.TIMEZ)
                cast(Kind.DATE)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.CHAR)
                cast(Kind.CLOB)
            }
            graph[Kind.TIMEZ.ordinal] = relationships {
                cast(Kind.TIMESTAMPZ)
                cast(Kind.TIMESTAMP)
                cast(Kind.TIMEZ)
                cast(Kind.TIME)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.CHAR)
                cast(Kind.CLOB)
            }
            graph[Kind.TIME.ordinal] = relationships {
                cast(Kind.TIMESTAMPZ)
                cast(Kind.TIMESTAMP)
                cast(Kind.TIMEZ)
                cast(Kind.TIME)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.CHAR)
                cast(Kind.CLOB)
            }
            graph[Kind.TIMESTAMPZ.ordinal] = relationships {
                cast(Kind.TIMESTAMPZ)
                cast(Kind.TIMESTAMP)
                cast(Kind.DATE)
                cast(Kind.TIMEZ)
                cast(Kind.TIME)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.CHAR)
                cast(Kind.CLOB)
            }
            graph[Kind.TIMESTAMP.ordinal] = relationships {
                cast(Kind.TIMESTAMPZ)
                cast(Kind.TIMESTAMP)
                cast(Kind.DATE)
                cast(Kind.TIMEZ)
                cast(Kind.TIME)
                cast(Kind.STRING)
                cast(Kind.VARCHAR)
                cast(Kind.CHAR)
                cast(Kind.CLOB)
            }
            graph[Kind.BAG.ordinal] = relationships {
                cast(Kind.BAG)
                cast(Kind.ARRAY)
            }
            graph[Kind.ARRAY.ordinal] = relationships {
                cast(Kind.BAG)
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
