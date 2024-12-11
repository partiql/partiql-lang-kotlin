package org.partiql.planner.internal.casts

import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Ref.Cast
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.types.PType

/**
 * A place to model type relationships (for now this is to answer CAST inquiries).
 *
 * @property types
 * @property graph      Going with a matrix here (using enum ordinals) as it's simple and avoids walking.
 */
internal class CastTable private constructor(
    private val types: Array<Int>,
    private val graph: Array<Array<Status>>,
) {

    fun get(operand: PType, target: PType): Cast? {
        val i = operand.code()
        val j = target.code()
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

        private val N = PType.codes().size

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
            val types = PType.codes().toTypedArray()
            val graph = arrayOfNulls<Array<Status>>(N)
            for (type in types) {
                // initialize all with empty relationships
                graph[type] = Array(N) { Status.NO }
            }
            graph[PType.UNKNOWN] = relationships {
                PType.codes().map {
                    cast(it)
                }
            }
            graph[PType.VARIANT] = relationships {
                PType.codes().map {
                    cast(it)
                }
            }
            graph[PType.DYNAMIC] = relationships {
                cast(PType.DYNAMIC)
                PType.codes().filterNot { it == PType.DYNAMIC }.forEach {
                    cast(it)
                }
            }
            graph[PType.BOOL] = relationships {
                cast(PType.BOOL)
                cast(PType.TINYINT)
                cast(PType.SMALLINT)
                cast(PType.INTEGER)
                cast(PType.BIGINT)
                cast(PType.NUMERIC)
                cast(PType.DECIMAL)
                cast(PType.REAL)
                cast(PType.DOUBLE)
                cast(PType.CHAR)
                cast(PType.STRING)
                cast(PType.VARCHAR)
            }
            graph[PType.TINYINT] = relationships {
                cast(PType.BOOL)
                cast(PType.TINYINT)
                cast(PType.SMALLINT)
                cast(PType.INTEGER)
                cast(PType.BIGINT)
                cast(PType.NUMERIC)
                cast(PType.DECIMAL)
                cast(PType.REAL)
                cast(PType.DOUBLE)
                cast(PType.STRING)
                cast(PType.VARCHAR)
                cast(PType.CHAR)
            }
            graph[PType.SMALLINT] = relationships {
                cast(PType.BOOL)
                cast(PType.TINYINT)
                cast(PType.SMALLINT)
                cast(PType.INTEGER)
                cast(PType.BIGINT)
                cast(PType.NUMERIC)
                cast(PType.DECIMAL)
                cast(PType.REAL)
                cast(PType.DOUBLE)
                cast(PType.STRING)
                cast(PType.CHAR)
                cast(PType.VARCHAR)
            }
            graph[PType.INTEGER] = relationships {
                cast(PType.BOOL)
                cast(PType.TINYINT)
                cast(PType.SMALLINT)
                cast(PType.INTEGER)
                cast(PType.BIGINT)
                cast(PType.NUMERIC)
                cast(PType.DECIMAL)
                cast(PType.REAL)
                cast(PType.DOUBLE)
                cast(PType.STRING)
                cast(PType.CHAR)
                cast(PType.VARCHAR)
            }
            graph[PType.BIGINT] = relationships {
                cast(PType.BOOL)
                cast(PType.TINYINT)
                cast(PType.SMALLINT)
                cast(PType.INTEGER)
                cast(PType.BIGINT)
                cast(PType.NUMERIC)
                cast(PType.DECIMAL)
                cast(PType.REAL)
                cast(PType.DOUBLE)
                cast(PType.STRING)
                cast(PType.CHAR)
                cast(PType.VARCHAR)
            }
            graph[PType.NUMERIC] = relationships {
                cast(PType.BOOL)
                cast(PType.TINYINT)
                cast(PType.SMALLINT)
                cast(PType.INTEGER)
                cast(PType.BIGINT)
                cast(PType.NUMERIC)
                cast(PType.DECIMAL)
                cast(PType.REAL)
                cast(PType.DOUBLE)
                cast(PType.STRING)
                cast(PType.CHAR)
                cast(PType.VARCHAR)
            }
            graph[PType.DECIMAL] = relationships {
                cast(PType.BOOL)
                cast(PType.TINYINT)
                cast(PType.SMALLINT)
                cast(PType.INTEGER)
                cast(PType.BIGINT)
                cast(PType.NUMERIC)
                cast(PType.DECIMAL)
                cast(PType.REAL)
                cast(PType.DOUBLE)
                cast(PType.STRING)
                cast(PType.CHAR)
                cast(PType.VARCHAR)
            }
            graph[PType.REAL] = relationships {
                cast(PType.BOOL)
                cast(PType.TINYINT)
                cast(PType.SMALLINT)
                cast(PType.INTEGER)
                cast(PType.BIGINT)
                cast(PType.NUMERIC)
                cast(PType.DECIMAL)
                cast(PType.REAL)
                cast(PType.DOUBLE)
                cast(PType.STRING)
                cast(PType.CHAR)
                cast(PType.VARCHAR)
            }
            graph[PType.DOUBLE] = relationships {
                cast(PType.BOOL)
                cast(PType.TINYINT)
                cast(PType.SMALLINT)
                cast(PType.INTEGER)
                cast(PType.BIGINT)
                cast(PType.NUMERIC)
                cast(PType.DECIMAL)
                cast(PType.REAL)
                cast(PType.DOUBLE)
                cast(PType.STRING)
                cast(PType.CHAR)
                cast(PType.VARCHAR)
            }
            graph[PType.CHAR] = relationships {
                PType.codes().filterNot {
                    it in setOf(PType.BLOB, PType.UNKNOWN, PType.DYNAMIC, PType.ARRAY, PType.BAG, PType.ROW)
                }.forEach {
                    cast(it)
                }
            }
            graph[PType.STRING] = relationships {
                PType.codes().filterNot {
                    it in setOf(PType.BLOB, PType.UNKNOWN, PType.DYNAMIC, PType.ARRAY, PType.BAG, PType.ROW)
                }.forEach {
                    cast(it)
                }
            }
            graph[PType.VARCHAR] = relationships {
                PType.codes().filterNot {
                    it in setOf(PType.BLOB, PType.UNKNOWN, PType.DYNAMIC, PType.ARRAY, PType.BAG, PType.ROW)
                }.forEach {
                    cast(it)
                }
            }
            graph[PType.CLOB] = relationships {
                PType.codes().filterNot {
                    it in setOf(PType.BLOB, PType.UNKNOWN, PType.DYNAMIC, PType.ARRAY, PType.BAG, PType.ROW)
                }.forEach {
                    cast(it)
                }
            }
            graph[PType.BLOB] = Array(N) { Status.NO }
            graph[PType.DATE] = relationships {
                cast(PType.TIMESTAMPZ)
                cast(PType.TIMESTAMP)
                cast(PType.TIMEZ)
                cast(PType.DATE)
                cast(PType.STRING)
                cast(PType.VARCHAR)
                cast(PType.CHAR)
                cast(PType.CLOB)
            }
            graph[PType.TIMEZ] = relationships {
                cast(PType.TIMESTAMPZ)
                cast(PType.TIMESTAMP)
                cast(PType.TIMEZ)
                cast(PType.TIME)
                cast(PType.STRING)
                cast(PType.VARCHAR)
                cast(PType.CHAR)
                cast(PType.CLOB)
            }
            graph[PType.TIME] = relationships {
                cast(PType.TIMESTAMPZ)
                cast(PType.TIMESTAMP)
                cast(PType.TIMEZ)
                cast(PType.TIME)
                cast(PType.STRING)
                cast(PType.VARCHAR)
                cast(PType.CHAR)
                cast(PType.CLOB)
            }
            graph[PType.TIMESTAMPZ] = relationships {
                cast(PType.TIMESTAMPZ)
                cast(PType.TIMESTAMP)
                cast(PType.DATE)
                cast(PType.TIMEZ)
                cast(PType.TIME)
                cast(PType.STRING)
                cast(PType.VARCHAR)
                cast(PType.CHAR)
                cast(PType.CLOB)
            }
            graph[PType.TIMESTAMP] = relationships {
                cast(PType.TIMESTAMPZ)
                cast(PType.TIMESTAMP)
                cast(PType.DATE)
                cast(PType.TIMEZ)
                cast(PType.TIME)
                cast(PType.STRING)
                cast(PType.VARCHAR)
                cast(PType.CHAR)
                cast(PType.CLOB)
            }
            graph[PType.BAG] = relationships {
                cast(PType.BAG)
                cast(PType.ARRAY)
            }
            graph[PType.ARRAY] = relationships {
                cast(PType.BAG)
                cast(PType.ARRAY)
            }
            graph[PType.STRUCT] = relationships {
                cast(PType.STRUCT)
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

        fun cast(target: Int) {
            relationships[target] = Status.YES
        }
    }
}
