package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.typer.PlanTyper.Companion.anyOf
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.types.PType
import org.partiql.types.PType.Kind
import org.partiql.value.MissingValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.charValue
import org.partiql.value.clobValue
import org.partiql.value.dateValue
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.listValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue

/**
 * Graph of super types for quick lookup because we don't have a tree.
 */
internal typealias SuperGraph = Array<Array<Kind?>>

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
    @OptIn(PartiQLValueExperimental::class)
    private fun Rex.isLiteralNull(): Boolean {
        val op = this.op
        return op is Rex.Op.Lit && op.value.isNull
    }

    /**
     * Checks for literal MISSING
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun Rex.isLiteralMissing(): Boolean {
        val op = this.op
        return op is Rex.Op.Lit && op.value is MissingValue
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
    @OptIn(PartiQLValueExperimental::class)
    fun mapping(): Pair<CompilerType, List<Mapping?>?> {
        var s = supertype ?: return CompilerType(PType.dynamic()) to null
        val superTypeBase = s.kind
        // If at top supertype, then return union of all accumulated types
        if (superTypeBase == Kind.DYNAMIC) {
            return anyOf(types)!!.toCType() to null
        }
        // If a collection, then return union of all accumulated types as these coercion rules are not defined by SQL.
        if (superTypeBase in setOf(Kind.ROW, Kind.STRUCT, Kind.BAG, Kind.ARRAY)) {
            return anyOf(types)!!.toCType() to null
        }
        // Decimal
        if (superTypeBase == Kind.DECIMAL) {
            val type = computeDecimal()
            if (type != null) {
                s = type
            }
        }
        // Text
        if (superTypeBase in setOf(Kind.CHAR, Kind.VARCHAR, Kind.STRING)) {
            val type = computeText()
            if (type != null) {
                s = type
            }
        }
        // If not initialized, then return null, missing, or null|missing.
        // Otherwise, return the supertype along with the coercion mapping
        val mapping = args.map {
            when {
                it.isLiteralNull() -> Mapping.Replacement(Rex(s, Rex.Op.Lit(nullValue(s.kind))))
                it.isLiteralMissing() -> Mapping.Replacement(Rex(s, Rex.Op.Lit(missingValue())))
                it.type == s -> Mapping.Coercion(s)
                else -> null
            }
        }
        return s to mapping
    }

    // TODO: Fix the computation of the supertype: https://github.com/partiql/partiql-lang-kotlin/issues/1566
    private fun computeDecimal(): CompilerType? {
        val (precision, scale) = types.fold((0 to 0)) { acc, type ->
            if (type.kind != Kind.DECIMAL) {
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
            if (type.kind !in setOf(Kind.VARCHAR, Kind.CHAR, Kind.STRING)) {
                return null
            }
            when (type.kind) {
                Kind.STRING -> {
                    containsString = true
                    Int.MAX_VALUE
                }
                Kind.VARCHAR -> {
                    containsVarChar = true
                    Math.max(acc, type.length)
                }
                Kind.CHAR -> {
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
        if (s.kind == Kind.DYNAMIC) return
        // Lookup and set the new minimum common supertype
        supertype = when {
            type.kind == Kind.DYNAMIC -> type
            s == type -> return // skip
            else -> graph[s.kind.ordinal][type.kind.ordinal]?.toPType() ?: CompilerType(PType.dynamic()) // lookup, if missing then go to top.
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
        private val N = Kind.values().size

        @JvmStatic
        private fun edges(vararg edges: Pair<Kind, Kind>): Array<Kind?> {
            val arr = arrayOfNulls<Kind?>(N)
            for (type in edges) {
                arr[type.first.ordinal] = type.second
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
            val graph = arrayOfNulls<Array<Kind?>>(N)
            for (type in Kind.values()) {
                // initialize all with empty edges
                graph[type.ordinal] = arrayOfNulls(N)
            }
            graph[Kind.DYNAMIC.ordinal] = edges()
            graph[Kind.BOOL.ordinal] = edges(
                Kind.BOOL to Kind.BOOL
            )
            graph[Kind.TINYINT.ordinal] = edges(
                Kind.TINYINT to Kind.TINYINT,
                Kind.SMALLINT to Kind.SMALLINT,
                Kind.INTEGER to Kind.INTEGER,
                Kind.BIGINT to Kind.BIGINT,
                Kind.NUMERIC to Kind.NUMERIC,
                Kind.DECIMAL to Kind.DECIMAL,
                Kind.REAL to Kind.REAL,
                Kind.DOUBLE to Kind.DOUBLE,
            )
            graph[Kind.SMALLINT.ordinal] = edges(
                Kind.TINYINT to Kind.SMALLINT,
                Kind.SMALLINT to Kind.SMALLINT,
                Kind.INTEGER to Kind.INTEGER,
                Kind.BIGINT to Kind.BIGINT,
                Kind.NUMERIC to Kind.NUMERIC,
                Kind.DECIMAL to Kind.DECIMAL,
                Kind.REAL to Kind.REAL,
                Kind.DOUBLE to Kind.DOUBLE,
            )
            graph[Kind.INTEGER.ordinal] = edges(
                Kind.TINYINT to Kind.INTEGER,
                Kind.SMALLINT to Kind.INTEGER,
                Kind.INTEGER to Kind.INTEGER,
                Kind.BIGINT to Kind.BIGINT,
                Kind.NUMERIC to Kind.NUMERIC,
                Kind.DECIMAL to Kind.DECIMAL,
                Kind.REAL to Kind.REAL,
                Kind.DOUBLE to Kind.DOUBLE,
            )
            graph[Kind.BIGINT.ordinal] = edges(
                Kind.TINYINT to Kind.BIGINT,
                Kind.SMALLINT to Kind.BIGINT,
                Kind.INTEGER to Kind.BIGINT,
                Kind.BIGINT to Kind.BIGINT,
                Kind.NUMERIC to Kind.NUMERIC,
                Kind.DECIMAL to Kind.DECIMAL,
                Kind.REAL to Kind.REAL,
                Kind.DOUBLE to Kind.DOUBLE,
            )
            graph[Kind.NUMERIC.ordinal] = edges(
                Kind.TINYINT to Kind.NUMERIC,
                Kind.SMALLINT to Kind.NUMERIC,
                Kind.INTEGER to Kind.NUMERIC,
                Kind.BIGINT to Kind.NUMERIC,
                Kind.NUMERIC to Kind.NUMERIC,
                Kind.DECIMAL to Kind.DECIMAL,
                Kind.REAL to Kind.REAL,
                Kind.DOUBLE to Kind.DOUBLE,
            )
            graph[Kind.DECIMAL.ordinal] = edges(
                Kind.TINYINT to Kind.DECIMAL,
                Kind.SMALLINT to Kind.DECIMAL,
                Kind.INTEGER to Kind.DECIMAL,
                Kind.BIGINT to Kind.DECIMAL,
                Kind.NUMERIC to Kind.DECIMAL,
                Kind.DECIMAL to Kind.DECIMAL,
                Kind.REAL to Kind.REAL,
                Kind.DOUBLE to Kind.DOUBLE,
            )
            graph[Kind.REAL.ordinal] = edges(
                Kind.TINYINT to Kind.REAL,
                Kind.SMALLINT to Kind.REAL,
                Kind.INTEGER to Kind.REAL,
                Kind.BIGINT to Kind.REAL,
                Kind.NUMERIC to Kind.REAL,
                Kind.DECIMAL to Kind.REAL,
                Kind.REAL to Kind.REAL,
                Kind.DOUBLE to Kind.DOUBLE,
            )
            graph[Kind.DOUBLE.ordinal] = edges(
                Kind.TINYINT to Kind.DOUBLE,
                Kind.SMALLINT to Kind.DOUBLE,
                Kind.INTEGER to Kind.DOUBLE,
                Kind.BIGINT to Kind.DOUBLE,
                Kind.NUMERIC to Kind.DOUBLE,
                Kind.DECIMAL to Kind.DOUBLE,
                Kind.REAL to Kind.DOUBLE,
                Kind.DOUBLE to Kind.DOUBLE,
            )
            graph[Kind.CHAR.ordinal] = edges(
                Kind.CHAR to Kind.CHAR,
                Kind.STRING to Kind.STRING,
                Kind.VARCHAR to Kind.STRING,
                Kind.CLOB to Kind.CLOB,
            )
            graph[Kind.STRING.ordinal] = edges(
                Kind.CHAR to Kind.STRING,
                Kind.STRING to Kind.STRING,
                Kind.VARCHAR to Kind.STRING,
                Kind.CLOB to Kind.CLOB,
            )
            graph[Kind.VARCHAR.ordinal] = edges(
                Kind.CHAR to Kind.VARCHAR,
                Kind.STRING to Kind.STRING,
                Kind.VARCHAR to Kind.VARCHAR,
                Kind.CLOB to Kind.CLOB,
            )
            graph[Kind.BLOB.ordinal] = edges(
                Kind.BLOB to Kind.BLOB,
            )
            graph[Kind.DATE.ordinal] = edges(
                Kind.DATE to Kind.DATE,
            )
            graph[Kind.CLOB.ordinal] = edges(
                Kind.CHAR to Kind.CLOB,
                Kind.STRING to Kind.CLOB,
                Kind.VARCHAR to Kind.CLOB,
                Kind.CLOB to Kind.CLOB,
            )
            graph[Kind.TIME.ordinal] = edges(
                Kind.TIME to Kind.TIME,
            )
            graph[Kind.TIMEZ.ordinal] = edges(
                Kind.TIMEZ to Kind.TIMEZ,
            )
            graph[Kind.TIMESTAMP.ordinal] = edges(
                Kind.TIMESTAMP to Kind.TIMESTAMP,
            )
            graph[Kind.TIMESTAMPZ.ordinal] = edges(
                Kind.TIMESTAMPZ to Kind.TIMESTAMPZ,
            )
            graph[Kind.ARRAY.ordinal] = edges(
                Kind.ARRAY to Kind.ARRAY,
                Kind.BAG to Kind.BAG,
            )
            graph[Kind.BAG.ordinal] = edges(
                Kind.ARRAY to Kind.BAG,
                Kind.BAG to Kind.BAG,
            )
            graph[Kind.STRUCT.ordinal] = edges(
                Kind.STRUCT to Kind.STRUCT,
            )
            graph[Kind.ROW.ordinal] = edges(
                Kind.ROW to Kind.ROW,
            )
            graph.requireNoNulls()
        }

        /**
         * TODO: We need to update the logic of this whole file. We are currently limited by not using parameters
         *  of types.
         */
        private fun Kind.toPType(): CompilerType = when (this) {
            Kind.BOOL -> PType.bool()
            Kind.DYNAMIC -> PType.dynamic()
            Kind.TINYINT -> PType.tinyint()
            Kind.SMALLINT -> PType.smallint()
            Kind.INTEGER -> PType.integer()
            Kind.BIGINT -> PType.bigint()
            Kind.NUMERIC -> PType.numeric()
            Kind.DECIMAL -> PType.decimal() // TODO: To be updated.
            Kind.REAL -> PType.real()
            Kind.DOUBLE -> PType.doublePrecision()
            Kind.CHAR -> PType.character(255) // TODO: To be updated
            Kind.VARCHAR -> PType.varchar(255) // TODO: To be updated
            Kind.STRING -> PType.string()
            Kind.BLOB -> PType.blob(Int.MAX_VALUE) // TODO: To be updated
            Kind.CLOB -> PType.clob(Int.MAX_VALUE) // TODO: To be updated
            Kind.DATE -> PType.date()
            Kind.TIMEZ -> PType.timez(6) // TODO: To be updated
            Kind.TIME -> PType.time(6) // TODO: To be updated
            Kind.TIMESTAMPZ -> PType.timestampz(6) // TODO: To be updated
            Kind.TIMESTAMP -> PType.timestamp(6) // TODO: To be updated
            Kind.BAG -> PType.bag() // TODO: To be updated
            Kind.ARRAY -> PType.array() // TODO: To be updated
            Kind.ROW -> PType.row(emptyList()) // TODO: To be updated
            Kind.STRUCT -> PType.struct() // TODO: To be updated
            Kind.UNKNOWN -> PType.unknown() // TODO: To be updated
            Kind.VARIANT -> TODO("variant in dynamic typer")
        }.toCType()

        @OptIn(PartiQLValueExperimental::class)
        private fun nullValue(kind: Kind): PartiQLValue {
            return when (kind) {
                Kind.DYNAMIC -> nullValue()
                Kind.BOOL -> boolValue(null)
                Kind.TINYINT -> int8Value(null)
                Kind.SMALLINT -> int16Value(null)
                Kind.INTEGER -> int32Value(null)
                Kind.BIGINT -> int64Value(null)
                Kind.NUMERIC -> intValue(null)
                Kind.DECIMAL -> decimalValue(null)
                Kind.REAL -> float32Value(null)
                Kind.DOUBLE -> float64Value(null)
                Kind.CHAR -> charValue(null)
                Kind.VARCHAR -> TODO("No implementation of VAR CHAR")
                Kind.STRING -> stringValue(null)
                Kind.BLOB -> blobValue(null)
                Kind.CLOB -> clobValue(null)
                Kind.DATE -> dateValue(null)
                Kind.TIMEZ,
                Kind.TIME -> timeValue(null)
                Kind.TIMESTAMPZ,
                Kind.TIMESTAMP -> timestampValue(null)
                Kind.BAG -> bagValue<PartiQLValue>(null)
                Kind.ARRAY -> listValue<PartiQLValue>(null)
                Kind.ROW -> structValue<PartiQLValue>(null)
                Kind.STRUCT -> structValue<PartiQLValue>()
                Kind.UNKNOWN -> nullValue()
                Kind.VARIANT -> TODO("variant in dynamic typer")
            }
        }
    }
}
