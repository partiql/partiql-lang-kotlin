package org.partiql.eval.internal.helpers

import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField

/**
 * Runtime type resolver implementing SQL-99 Section 9.3 type coercion rules.
 * Used to determine the common supertype of a collection of runtime values.
 */
internal object DynamicTyper {

    /**
     * Returns the common super type of [lType] and [rType], or null if incompatible.
     */
    @JvmStatic
    fun getCommonSuperType(lType: PType, rType: PType): PType? {
        if (lType.code() == rType.code()) {
            return mergeParameterized(lType, rType)
        }
        if (lType.code() == PType.UNKNOWN) return rType
        if (rType.code() == PType.UNKNOWN) return lType
        if (lType.code() == PType.DYNAMIC || rType.code() == PType.DYNAMIC) return PType.dynamic()
        val coerced = coerceTypeBase(lType, rType.code())
        if (coerced != null) {
            return getCommonSuperType(coerced, rType)
        }
        val coercedReverse = coerceTypeBase(rType, lType.code())
        if (coercedReverse != null) {
            return getCommonSuperType(lType, coercedReverse)
        }
        return null
    }

    /**
     * Computes the common supertype across a list of [PType]s.
     * Returns [PType.dynamic] if the list is empty or types are incompatible.
     */
    @JvmStatic
    fun commonSuperType(types: List<PType>): PType {
        if (types.isEmpty()) return PType.dynamic()
        var common: PType = types.first()
        for (i in 1 until types.size) {
            val t = types[i]
            if (t.code() == PType.DYNAMIC) continue
            if (common.code() == PType.DYNAMIC) {
                common = t
                continue
            }
            common = getCommonSuperType(common, t) ?: return PType.dynamic()
        }
        return common
    }

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
                    val fieldName = if (af.name == bf.name) af.name else "_"
                    PTypeField.of(fieldName, fieldType)
                }
                PType.row(mergedFields)
            }
            else -> a
        }
    }
}
