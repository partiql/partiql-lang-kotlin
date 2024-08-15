@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Rex
import org.partiql.types.DecimalType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.value.MissingValue
import org.partiql.value.NullValue
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
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.STRUCT
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP
import kotlin.math.max

/**
 * Graph of super types for quick lookup because we don't have a tree.
 */
internal typealias SuperGraph = Array<Array<PartiQLValueType?>>

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
@OptIn(PartiQLValueExperimental::class)
internal class DynamicTyper {

    private var supertype: PartiQLValueType? = null
    private var args = mutableListOf<PartiQLValueType>()

    private val types = mutableSetOf<StaticType>()

    /**
     * Adds the [rex]'s [Rex.type] to the typing accumulator (if the [rex] is not a literal NULL/MISSING).
     */
    fun accumulate(rex: Rex) {
        when (rex.isLiteralAbsent()) {
            true -> accumulateUnknown()
            false -> accumulate(rex.type)
        }
    }

    /**
     * Checks for literal NULL or MISSING.
     */
    private fun Rex.isLiteralAbsent(): Boolean {
        val op = this.op
        return op is Rex.Op.Lit && (op.value is MissingValue || op.value is NullValue)
    }

    /**
     * When a literal null or missing value is present in the query, its type is unknown. Therefore, its type must be
     * inferred. This function ignores literal null/missing values, yet adds their indices to know how to return the
     * mapping.
     */
    private fun accumulateUnknown() {
        args.add(ANY)
    }

    /**
     * This adds non-absent types (aka not NULL / MISSING literals) to the typing accumulator.
     * @param type
     */
    private fun accumulate(type: StaticType) {
        val flatType = type.flatten()
        if (flatType == StaticType.ANY) {
            types.add(flatType)
            args.add(ANY)
            calculate(ANY)
            return
        }
        val allTypes = flatType.allTypes
        when (allTypes.size) {
            0 -> {
                error("This should not have happened.")
            }
            1 -> {
                // Had single type
                val single = allTypes.first()
                val singleRuntime = single.toRuntimeType()
                types.add(single)
                args.add(singleRuntime)
                calculate(singleRuntime)
            }
            else -> {
                // Had a union; use ANY runtime
                types.addAll(allTypes)
                args.add(ANY)
                calculate(ANY)
            }
        }
    }

    /**
     * Returns a pair of the return StaticType and the coercion.
     *
     * If the list is null, then no mapping is required.
     *
     * @return
     */
    fun mapping(): Pair<StaticType, List<Pair<PartiQLValueType, PartiQLValueType>>?> {
        // no coercion
        val s = when (val superT = supertype) {
            // If not initialized, then return null, missing, or null|missing.
            null -> return StaticType.ANY to null
            // If at top supertype, then return union of all accumulated types
            ANY -> return StaticType.unionOf(types).flatten() to null
            // If a collection, then return union of all accumulated types as these coercion rules are not defined by SQL.
            STRUCT, BAG, LIST, SEXP -> return StaticType.unionOf(types) to null
            DECIMAL -> {
                val type = computeDecimal()
                // coercion required. fall back
                if (type == null) superT else return type to null
            }
            STRING -> {
                val type = computeString()
                // coercion required. fall back
                if (type == null) superT else return type to null
            }
            else -> superT
        }

        // Otherwise, return the supertype along with the coercion mapping
        val type = s.toStaticType()
        val mapping = args.map { it to s }
        return type to mapping
    }

    private fun computeDecimal(): DecimalType? {
        val (precision, scale) = types.fold((0 to 0)) { acc, staticType ->
            val decimalType = staticType as? DecimalType ?: return null
            val constr = decimalType.precisionScaleConstraint as DecimalType.PrecisionScaleConstraint.Constrained
            val precision = max(constr.precision, acc.first)
            val scale = max(constr.scale, acc.second)
            precision to scale
        }
        return DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(precision, scale))
    }

    private fun computeString(): StringType? {
        val (length, isVarchar) = types.fold((0 to false)) { acc, staticType ->
            staticType as? StringType ?: return null
            when (val constr = staticType.lengthConstraint) {
                is StringType.StringLengthConstraint.Constrained -> return@fold when (val l = constr.length) {
                    is NumberConstraint.Equals -> max(acc.first, l.value) to acc.second
                    is NumberConstraint.UpTo -> max(acc.first, l.value) to true
                }
                StringType.StringLengthConstraint.Unconstrained -> return StaticType.STRING
            }
        }
        return when (isVarchar) {
            true -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(length)))
            false -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(length)))
        }
    }

    private fun calculate(type: PartiQLValueType) {
        val s = supertype
        // Initialize
        if (s == null) {
            supertype = type
            return
        }
        // Don't bother calculating the new supertype, we've already hit `dynamic`.
        if (s == ANY) return
        // Lookup and set the new minimum common supertype
        supertype = when {
            type == ANY -> type
            s == type -> return // skip
            else -> graph[s][type] ?: ANY // lookup, if missing then go to top.
        }
    }

    private operator fun <T> Array<T>.get(t: PartiQLValueType): T = get(t.ordinal)

    /**
     * !! IMPORTANT !!
     *
     * This is duplicated from the TypeLattice because that was removed in v1.0.0. I wanted to implement this as
     * a standalone component so that it is easy to merge (and later merge with CastTable) into v1.0.0.
     */
    companion object {

        private operator fun <T> Array<T>.set(t: PartiQLValueType, value: T): Unit = this.set(t.ordinal, value)

        @JvmStatic
        private val N = PartiQLValueType.values().size

        @JvmStatic
        private fun edges(vararg edges: Pair<PartiQLValueType, PartiQLValueType>): Array<PartiQLValueType?> {
            val arr = arrayOfNulls<PartiQLValueType?>(N)
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
            val graph = arrayOfNulls<Array<PartiQLValueType?>>(N)
            for (type in PartiQLValueType.values()) {
                // initialize all with empty edges
                graph[type] = arrayOfNulls(N)
            }
            graph[ANY] = edges()
            graph[BOOL] = edges(
                BOOL to BOOL
            )
            graph[INT8] = edges(
                INT8 to INT8,
                INT16 to INT16,
                INT32 to INT32,
                INT64 to INT64,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[INT16] = edges(
                INT8 to INT16,
                INT16 to INT16,
                INT32 to INT32,
                INT64 to INT64,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[INT32] = edges(
                INT8 to INT32,
                INT16 to INT32,
                INT32 to INT32,
                INT64 to INT64,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[INT64] = edges(
                INT8 to INT64,
                INT16 to INT64,
                INT32 to INT64,
                INT64 to INT64,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[INT] = edges(
                INT8 to INT,
                INT16 to INT,
                INT32 to INT,
                INT64 to INT,
                INT to INT,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[DECIMAL] = edges(
                INT8 to DECIMAL,
                INT16 to DECIMAL,
                INT32 to DECIMAL,
                INT64 to DECIMAL,
                INT to DECIMAL,
                DECIMAL to DECIMAL,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[DECIMAL_ARBITRARY] = edges(
                INT8 to DECIMAL_ARBITRARY,
                INT16 to DECIMAL_ARBITRARY,
                INT32 to DECIMAL_ARBITRARY,
                INT64 to DECIMAL_ARBITRARY,
                INT to DECIMAL_ARBITRARY,
                DECIMAL to DECIMAL_ARBITRARY,
                DECIMAL_ARBITRARY to DECIMAL_ARBITRARY,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[FLOAT32] = edges(
                INT8 to FLOAT32,
                INT16 to FLOAT32,
                INT32 to FLOAT32,
                INT64 to FLOAT32,
                INT to FLOAT32,
                DECIMAL to FLOAT32,
                DECIMAL_ARBITRARY to FLOAT32,
                FLOAT32 to FLOAT32,
                FLOAT64 to FLOAT64,
            )
            graph[FLOAT64] = edges(
                INT8 to FLOAT64,
                INT16 to FLOAT64,
                INT32 to FLOAT64,
                INT64 to FLOAT64,
                INT to FLOAT64,
                DECIMAL to FLOAT64,
                DECIMAL_ARBITRARY to FLOAT64,
                FLOAT32 to FLOAT64,
                FLOAT64 to FLOAT64,
            )
            graph[CHAR] = edges(
                CHAR to CHAR,
                STRING to STRING,
                SYMBOL to STRING,
                CLOB to CLOB,
            )
            graph[STRING] = edges(
                CHAR to STRING,
                STRING to STRING,
                SYMBOL to STRING,
                CLOB to CLOB,
            )
            graph[SYMBOL] = edges(
                CHAR to SYMBOL,
                STRING to STRING,
                SYMBOL to SYMBOL,
                CLOB to CLOB,
            )
            graph[BINARY] = edges(
                BINARY to BINARY,
            )
            graph[BYTE] = edges(
                BYTE to BYTE,
                BLOB to BLOB,
            )
            graph[BLOB] = edges(
                BYTE to BLOB,
                BLOB to BLOB,
            )
            graph[DATE] = edges(
                DATE to DATE,
            )
            graph[CLOB] = edges(
                CHAR to CLOB,
                STRING to CLOB,
                SYMBOL to CLOB,
                CLOB to CLOB,
            )
            graph[TIME] = edges(
                TIME to TIME,
            )
            graph[TIMESTAMP] = edges(
                TIMESTAMP to TIMESTAMP,
            )
            graph[INTERVAL] = edges(
                INTERVAL to INTERVAL,
            )
            graph[LIST] = edges(
                LIST to LIST,
                SEXP to SEXP,
                BAG to BAG,
            )
            graph[SEXP] = edges(
                LIST to SEXP,
                SEXP to SEXP,
                BAG to BAG,
            )
            graph[BAG] = edges(
                LIST to BAG,
                SEXP to BAG,
                BAG to BAG,
            )
            graph[STRUCT] = edges(
                STRUCT to STRUCT,
            )
            graph.requireNoNulls()
        }
    }
}
