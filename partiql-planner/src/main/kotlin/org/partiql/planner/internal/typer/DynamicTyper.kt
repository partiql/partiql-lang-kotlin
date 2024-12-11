package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.typer.PlanTyper.Companion.anyOf
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Graph of super types for quick lookup because we don't have a tree.
 */
internal typealias SuperGraph = Array<Array<Int?>>

/**
 * For lack of a better name, this is the "dynamic typer" which implements the typing rules of SQL-99 9.3.
 *
 * SQL-99 9.3 Data types of results of aggregations (<case-when>, <collection value expression>, <query expression>)
 *  > https://web.cecs.pdx.edu/~len/sql1999.pdf#page=359
 *
 * Usage,
 *  To calculate the type of an "aggregation" create a new instance and "accumulate" each possible type.
 *  This is a pain with StaticType...
 */
internal class DynamicTyper {

    private var supertype: CompilerType? = null
    private var args = mutableListOf<Rex>()
    private val types = mutableListOf<CompilerType>()

    /**
     * Adds the [rex]'s [Rex.type] to the typing accumulator (if the [rex] is not a literal NULL/MISSING).
     */
    fun accumulate(rex: Rex) {
        when {
            rex.isLiteralNull() || rex.isLiteralMissing() -> accumulateUnknown(rex)
            else -> accumulateConcrete(rex)
        }
    }

    /**
     * Checks for literal NULL
     */
    private fun Rex.isLiteralNull(): Boolean {
        val op = this.op
        return op is Rex.Op.Lit && op.value.isNull
    }

    /**
     * Checks for literal MISSING
     */
    private fun Rex.isLiteralMissing(): Boolean {
        val op = this.op
        return op is Rex.Op.Lit && op.value.isMissing
    }

    /**
     * When a literal null or missing value is present in the query, its type is unknown. Therefore, its type must be
     * inferred. This function ignores literal null/missing values, yet adds their indices to know how to return the
     * mapping.
     */
    private fun accumulateUnknown(rex: Rex) {
        args.add(rex)
    }

    /**
     * This adds non-absent types (aka not NULL / MISSING literals) to the typing accumulator.
     * @param type
     */
    private fun accumulateConcrete(rex: Rex) {
        types.add(rex.type)
        args.add(rex)
        calculate(rex.type)
    }

    /**
     * Returns a pair of the return type and the coercions.
     *
     * If the list is null, then no mapping is required.
     *
     * @return
     */
    fun mapping(): Pair<CompilerType, List<Mapping?>?> {
        var s = supertype ?: return CompilerType(PType.dynamic()) to null
        val superTypeBase = s.code()
        // If at top supertype, then return union of all accumulated types
        if (superTypeBase == PType.DYNAMIC) {
            return anyOf(types)!!.toCType() to null
        }
        // If a collection, then return union of all accumulated types as these coercion rules are not defined by SQL.
        if (superTypeBase in setOf(PType.ROW, PType.STRUCT, PType.BAG, PType.ARRAY)) {
            return anyOf(types)!!.toCType() to null
        }
        // Decimal
        if (superTypeBase == PType.DECIMAL) {
            val type = computeDecimal()
            if (type != null) {
                s = type
            }
        }
        // Text
        if (superTypeBase in setOf(PType.CHAR, PType.VARCHAR, PType.STRING)) {
            val type = computeText()
            if (type != null) {
                s = type
            }
        }
        // If not initialized, then return null, missing, or null|missing.
        // Otherwise, return the supertype along with the coercion mapping
        val mapping = args.map {
            when {
                it.isLiteralNull() -> Mapping.Replacement(Rex(s, Rex.Op.Lit(Datum.nullValue(PType.of(s.code())))))
                it.isLiteralMissing() -> Mapping.Replacement(Rex(s, Rex.Op.Lit(Datum.missing(PType.of(s.code())))))
                it.type == s -> Mapping.Coercion(s)
                else -> null
            }
        }
        return s to mapping
    }

    // TODO: Fix the computation of the supertype: https://github.com/partiql/partiql-lang-kotlin/issues/1566
    private fun computeDecimal(): CompilerType? {
        val (precision, scale) = types.fold((0 to 0)) { acc, type ->
            if (type.code() != PType.DECIMAL) {
                return null
            }
            val precision = Math.max(type.precision, acc.first)
            val scale = Math.max(type.scale, acc.second)
            precision to scale
        }
        return PType.decimal(precision, scale).toCType()
    }

    private fun computeText(): CompilerType? {
        var containsString = false
        var containsVarChar = false
        val length = types.fold(0) { acc, type ->
            if (type.code() !in setOf(PType.VARCHAR, PType.CHAR, PType.STRING)) {
                return null
            }
            when (type.code()) {
                PType.STRING -> {
                    containsString = true
                    Int.MAX_VALUE
                }
                PType.VARCHAR -> {
                    containsVarChar = true
                    Math.max(acc, type.length)
                }
                PType.CHAR -> {
                    Math.max(acc, type.length)
                }
                else -> error("Received type: $type")
            }
        }
        return when {
            containsString -> PType.string()
            containsVarChar -> PType.varchar(length)
            else -> PType.character(length)
        }.toCType()
    }

    internal sealed interface Mapping {
        class Replacement(val replacement: Rex) : Mapping
        class Coercion(val target: CompilerType) : Mapping
    }

    private fun calculate(type: CompilerType) {
        val s = supertype
        // Initialize
        if (s == null) {
            supertype = type
            return
        }
        // Don't bother calculating the new supertype, we've already hit `dynamic`.
        if (s.code() == PType.DYNAMIC) return
        // Lookup and set the new minimum common supertype
        supertype = when {
            type.code() == PType.DYNAMIC -> type
            s == type -> return // skip
            else -> graph[s.code()][type.code()]?.toPType() ?: CompilerType(PType.dynamic()) // lookup, if missing then go to top.
        }
    }

    /**
     * !! IMPORTANT !!
     *
     * This is duplicated from the TypeLattice because that was removed in v1.0.0. I wanted to implement this as
     * a standalone component so that it is easy to merge (and later merge with CastTable) into v1.0.0.
     */
    companion object {

        @JvmStatic
        private val N = PType.codes().size

        @JvmStatic
        private fun edges(vararg edges: Pair<Int, Int>): Array<Int?> {
            val arr = arrayOfNulls<Int?>(N)
            for (type in edges) {
                arr[type.first] = type.second
            }
            return arr
        }

        /**
         * This table defines the rules in the SQL-99 section 9.3 BUT we don't have type constraints yet.
         *
         * TODO collection supertypes
         * TODO datetime supertypes
         */
        @JvmStatic
        internal val graph: SuperGraph = run {
            val graph = arrayOfNulls<Array<Int?>>(N)
            for (type in PType.codes()) {
                // initialize all with empty edges
                graph[type] = arrayOfNulls(N)
            }
            graph[PType.DYNAMIC] = edges()
            graph[PType.BOOL] = edges(
                PType.BOOL to PType.BOOL
            )
            graph[PType.TINYINT] = edges(
                PType.TINYINT to PType.TINYINT,
                PType.SMALLINT to PType.SMALLINT,
                PType.INTEGER to PType.INTEGER,
                PType.BIGINT to PType.BIGINT,
                PType.NUMERIC to PType.NUMERIC,
                PType.DECIMAL to PType.DECIMAL,
                PType.REAL to PType.REAL,
                PType.DOUBLE to PType.DOUBLE,
            )
            graph[PType.SMALLINT] = edges(
                PType.TINYINT to PType.SMALLINT,
                PType.SMALLINT to PType.SMALLINT,
                PType.INTEGER to PType.INTEGER,
                PType.BIGINT to PType.BIGINT,
                PType.NUMERIC to PType.NUMERIC,
                PType.DECIMAL to PType.DECIMAL,
                PType.REAL to PType.REAL,
                PType.DOUBLE to PType.DOUBLE,
            )
            graph[PType.INTEGER] = edges(
                PType.TINYINT to PType.INTEGER,
                PType.SMALLINT to PType.INTEGER,
                PType.INTEGER to PType.INTEGER,
                PType.BIGINT to PType.BIGINT,
                PType.NUMERIC to PType.NUMERIC,
                PType.DECIMAL to PType.DECIMAL,
                PType.REAL to PType.REAL,
                PType.DOUBLE to PType.DOUBLE,
            )
            graph[PType.BIGINT] = edges(
                PType.TINYINT to PType.BIGINT,
                PType.SMALLINT to PType.BIGINT,
                PType.INTEGER to PType.BIGINT,
                PType.BIGINT to PType.BIGINT,
                PType.NUMERIC to PType.NUMERIC,
                PType.DECIMAL to PType.DECIMAL,
                PType.REAL to PType.REAL,
                PType.DOUBLE to PType.DOUBLE,
            )
            graph[PType.NUMERIC] = edges(
                PType.TINYINT to PType.NUMERIC,
                PType.SMALLINT to PType.NUMERIC,
                PType.INTEGER to PType.NUMERIC,
                PType.BIGINT to PType.NUMERIC,
                PType.NUMERIC to PType.NUMERIC,
                PType.DECIMAL to PType.DECIMAL,
                PType.REAL to PType.REAL,
                PType.DOUBLE to PType.DOUBLE,
            )
            graph[PType.DECIMAL] = edges(
                PType.TINYINT to PType.DECIMAL,
                PType.SMALLINT to PType.DECIMAL,
                PType.INTEGER to PType.DECIMAL,
                PType.BIGINT to PType.DECIMAL,
                PType.NUMERIC to PType.DECIMAL,
                PType.DECIMAL to PType.DECIMAL,
                PType.REAL to PType.REAL,
                PType.DOUBLE to PType.DOUBLE,
            )
            graph[PType.REAL] = edges(
                PType.TINYINT to PType.REAL,
                PType.SMALLINT to PType.REAL,
                PType.INTEGER to PType.REAL,
                PType.BIGINT to PType.REAL,
                PType.NUMERIC to PType.REAL,
                PType.DECIMAL to PType.REAL,
                PType.REAL to PType.REAL,
                PType.DOUBLE to PType.DOUBLE,
            )
            graph[PType.DOUBLE] = edges(
                PType.TINYINT to PType.DOUBLE,
                PType.SMALLINT to PType.DOUBLE,
                PType.INTEGER to PType.DOUBLE,
                PType.BIGINT to PType.DOUBLE,
                PType.NUMERIC to PType.DOUBLE,
                PType.DECIMAL to PType.DOUBLE,
                PType.REAL to PType.DOUBLE,
                PType.DOUBLE to PType.DOUBLE,
            )
            graph[PType.CHAR] = edges(
                PType.CHAR to PType.CHAR,
                PType.STRING to PType.STRING,
                PType.VARCHAR to PType.STRING,
                PType.CLOB to PType.CLOB,
            )
            graph[PType.STRING] = edges(
                PType.CHAR to PType.STRING,
                PType.STRING to PType.STRING,
                PType.VARCHAR to PType.STRING,
                PType.CLOB to PType.CLOB,
            )
            graph[PType.VARCHAR] = edges(
                PType.CHAR to PType.VARCHAR,
                PType.STRING to PType.STRING,
                PType.VARCHAR to PType.VARCHAR,
                PType.CLOB to PType.CLOB,
            )
            graph[PType.BLOB] = edges(
                PType.BLOB to PType.BLOB,
            )
            graph[PType.DATE] = edges(
                PType.DATE to PType.DATE,
            )
            graph[PType.CLOB] = edges(
                PType.CHAR to PType.CLOB,
                PType.STRING to PType.CLOB,
                PType.VARCHAR to PType.CLOB,
                PType.CLOB to PType.CLOB,
            )
            graph[PType.TIME] = edges(
                PType.TIME to PType.TIME,
            )
            graph[PType.TIMEZ] = edges(
                PType.TIMEZ to PType.TIMEZ,
            )
            graph[PType.TIMESTAMP] = edges(
                PType.TIMESTAMP to PType.TIMESTAMP,
            )
            graph[PType.TIMESTAMPZ] = edges(
                PType.TIMESTAMPZ to PType.TIMESTAMPZ,
            )
            graph[PType.ARRAY] = edges(
                PType.ARRAY to PType.ARRAY,
                PType.BAG to PType.BAG,
            )
            graph[PType.BAG] = edges(
                PType.ARRAY to PType.BAG,
                PType.BAG to PType.BAG,
            )
            graph[PType.STRUCT] = edges(
                PType.STRUCT to PType.STRUCT,
            )
            graph[PType.ROW] = edges(
                PType.ROW to PType.ROW,
            )
            graph.requireNoNulls()
        }

        /**
         * TODO: We need to update the logic of this whole file. We are currently limited by not using parameters
         *  of types.
         */
        private fun Int.toPType(): CompilerType = when (this) {
            PType.BOOL -> PType.bool()
            PType.DYNAMIC -> PType.dynamic()
            PType.TINYINT -> PType.tinyint()
            PType.SMALLINT -> PType.smallint()
            PType.INTEGER -> PType.integer()
            PType.BIGINT -> PType.bigint()
            PType.NUMERIC -> PType.numeric(38, 0) // TODO: To be updated
            PType.DECIMAL -> PType.decimal(38, 0) // TODO: To be updated.
            PType.REAL -> PType.real()
            PType.DOUBLE -> PType.doublePrecision()
            PType.CHAR -> PType.character(255) // TODO: To be updated
            PType.VARCHAR -> PType.varchar(255) // TODO: To be updated
            PType.STRING -> PType.string()
            PType.BLOB -> PType.blob(Int.MAX_VALUE) // TODO: To be updated
            PType.CLOB -> PType.clob(Int.MAX_VALUE) // TODO: To be updated
            PType.DATE -> PType.date()
            PType.TIMEZ -> PType.timez(6) // TODO: To be updated
            PType.TIME -> PType.time(6) // TODO: To be updated
            PType.TIMESTAMPZ -> PType.timestampz(6) // TODO: To be updated
            PType.TIMESTAMP -> PType.timestamp(6) // TODO: To be updated
            PType.BAG -> PType.bag() // TODO: To be updated
            PType.ARRAY -> PType.array() // TODO: To be updated
            PType.ROW -> PType.row(emptyList()) // TODO: To be updated
            PType.STRUCT -> PType.struct() // TODO: To be updated
            PType.UNKNOWN -> PType.unknown() // TODO: To be updated
            PType.VARIANT -> TODO("variant in dynamic typer")
            else -> error("Unknown type: $this")
        }.toCType()
    }
}
