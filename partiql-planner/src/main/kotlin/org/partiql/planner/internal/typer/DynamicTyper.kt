package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.typer.PlanTyper.Companion.anyOf
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum

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
     * @param rex
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
            val precision = type.precision.coerceAtLeast(acc.first)
            val scale = type.scale.coerceAtLeast(acc.second)
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
                    acc.coerceAtLeast(type.length)
                }
                PType.CHAR -> {
                    acc.coerceAtLeast(type.length)
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
        supertype = when {
            type.code() == PType.DYNAMIC -> type
            s == type -> return
            else -> getCommonSuperType(s, type)?.toCType() ?: CompilerType(PType.dynamic())
        }
    }

    companion object {

        /**
         * Returns the common super type of [lType] and [rType], or null if incompatible.
         *
         * Follows the pattern from Trino's TypeCoercion.compatibility:
         * 1. Equal types → return as-is
         * 2. UNKNOWN is compatible with everything
         * 3. DYNAMIC absorbs everything
         * 4. Same base type → merge parameters (precision, scale, length)
         * 5. Different base types → try coerceTypeBase in both directions, recurse
         */
        @JvmStatic
        fun getCommonSuperType(lType: PType, rType: PType): PType? {
            if (lType.code() == rType.code()) {
                return mergeParameterized(lType, rType)
            }
            if (lType.code() == PType.UNKNOWN) return rType
            if (rType.code() == PType.UNKNOWN) return lType
            if (lType.code() == PType.DYNAMIC || rType.code() == PType.DYNAMIC) return PType.dynamic()
            // Try coercing fromType → toType's base
            val coerced = coerceTypeBase(lType, rType.code())
            if (coerced != null) {
                return getCommonSuperType(coerced, rType)
            }
            // Try coercing toType → fromType's base
            val coercedReverse = coerceTypeBase(rType, lType.code())
            if (coercedReverse != null) {
                return getCommonSuperType(lType, coercedReverse)
            }
            return null
        }

        /**
         * Attempts to coerce [sourceType] to the target type base [targetCode].
         * Returns the coerced type with appropriate parameters, or null if not coercible.
         *
         * This follows the rules in the SQL-99 section 9.3.
         * Each source type declares which target base types it can be coerced to, returning the result type
         * with proper precision/scale/length.
         */
        private fun coerceTypeBase(sourceType: PType, targetCode: Int): PType? {
            if (sourceType.code() == targetCode) return sourceType
            return when (sourceType.code()) {
                PType.TINYINT -> when (targetCode) {
                    PType.SMALLINT -> PType.smallint()
                    PType.INTEGER -> PType.integer()
                    PType.BIGINT -> PType.bigint()
                    PType.NUMERIC -> PType.numeric(3, 0)
                    PType.DECIMAL -> PType.decimal(3, 0)
                    PType.REAL -> PType.real()
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
                PType.SMALLINT -> when (targetCode) {
                    PType.INTEGER -> PType.integer()
                    PType.BIGINT -> PType.bigint()
                    PType.NUMERIC -> PType.numeric(5, 0)
                    PType.DECIMAL -> PType.decimal(5, 0)
                    PType.REAL -> PType.real()
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
                PType.INTEGER -> when (targetCode) {
                    PType.BIGINT -> PType.bigint()
                    PType.NUMERIC -> PType.numeric(10, 0)
                    PType.DECIMAL -> PType.decimal(10, 0)
                    PType.REAL -> PType.real()
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
                PType.BIGINT -> when (targetCode) {
                    PType.NUMERIC -> PType.numeric(19, 0)
                    PType.DECIMAL -> PType.decimal(19, 0)
                    PType.REAL -> PType.real()
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
                PType.NUMERIC -> when (targetCode) {
                    PType.DECIMAL -> PType.decimal(sourceType.precision, sourceType.scale)
                    PType.REAL -> PType.real()
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
                PType.DECIMAL -> when (targetCode) {
                    PType.REAL -> PType.real()
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
                PType.REAL -> when (targetCode) {
                    PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
                PType.CHAR -> when (targetCode) {
                    PType.VARCHAR -> PType.varchar(sourceType.length)
                    PType.STRING -> PType.string()
                    PType.CLOB -> PType.clob(sourceType.length)
                    else -> null
                }
                PType.VARCHAR -> when (targetCode) {
                    PType.STRING -> PType.string()
                    PType.CLOB -> PType.clob(sourceType.length)
                    else -> null
                }
                PType.STRING -> when (targetCode) {
                    PType.CLOB -> PType.clob(Int.MAX_VALUE)
                    else -> null
                }
                PType.DATE -> when (targetCode) {
                    PType.TIMESTAMP -> PType.timestamp(0)
                    PType.TIMESTAMPZ -> PType.timestampz(0)
                    else -> null
                }
                PType.TIME -> when (targetCode) {
                    PType.TIMEZ -> PType.timez(sourceType.precision)
                    else -> null
                }
                PType.TIMESTAMP -> when (targetCode) {
                    PType.TIMESTAMPZ -> PType.timestampz(sourceType.precision)
                    else -> null
                }
                PType.ARRAY -> when (targetCode) {
                    PType.BAG -> PType.bag()
                    else -> null
                }
                else -> null
            }
        }

        /**
         * Merges two types with the same base type code, handling parameterized types.
         */
        private fun mergeParameterized(a: PType, b: PType): PType? {
            return when (a.code()) {
                PType.DECIMAL, PType.NUMERIC -> {
                    val scale = maxOf(a.scale, b.scale)
                    val precision = minOf(38, maxOf(a.precision - a.scale, b.precision - b.scale) + scale)
                    if (a.code() == PType.DECIMAL) PType.decimal(precision, scale) else PType.numeric(precision, scale)
                }
                PType.CHAR -> PType.character(maxOf(a.length, b.length))
                PType.VARCHAR -> PType.varchar(maxOf(a.length, b.length))
                PType.CLOB -> PType.clob(maxOf(a.length, b.length))
                PType.BLOB -> PType.blob(maxOf(a.length, b.length))
                PType.TIME -> PType.time(maxOf(a.precision, b.precision))
                PType.TIMEZ -> PType.timez(maxOf(a.precision, b.precision))
                PType.TIMESTAMP -> PType.timestamp(maxOf(a.precision, b.precision))
                PType.TIMESTAMPZ -> PType.timestampz(maxOf(a.precision, b.precision))
                PType.ARRAY -> {
                    val elementType = getCommonSuperType(a.typeParameter, b.typeParameter) ?: return null
                    PType.array(elementType)
                }
                PType.BAG -> {
                    val elementType = getCommonSuperType(a.typeParameter, b.typeParameter) ?: return null
                    PType.bag(elementType)
                }
                PType.ROW -> {
                    val aFields = a.fields.toList()
                    val bFields = b.fields.toList()
                    if (aFields.size != bFields.size) return null
                    val mergedFields = aFields.zip(bFields).map { (af, bf) ->
                        val fieldType = getCommonSuperType(af.type, bf.type) ?: return null
                        // We ignore name match in super type calculation for now.
                        // Likely in the future, we passed a flag whether to ignore name
                        val fieldName = if (af.name == bf.name) af.name else "_"
                        PTypeField.of(fieldName, fieldType)
                    }
                    PType.row(mergedFields)
                }
                else -> a
            }
        }
    }
}
