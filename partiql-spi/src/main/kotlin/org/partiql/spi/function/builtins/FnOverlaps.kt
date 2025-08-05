package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.function.builtins.internal.PErrors.cardinalityViolationException
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.FunctionUtils.isDateTimeType
import org.partiql.spi.utils.FunctionUtils.isIntervalType
import org.partiql.spi.value.Datum

/**
 * SQL Spec 1999: Section 8.12 <overlaps predicate>
 *
 * <overlaps predicate> ::=
 *    <row value expression 1> OVERLAPS <row value expression 2>
 * <row value expression 1> ::= <row value expression>
 * <row value expression 2> ::= <row value expression>
 */
internal object FnOverlaps : FnOverload() {
    val name = FunctionUtils.hide("overlaps")
    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(name, listOf(PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<out PType>): Fn? {
        val arg0 = args[0]
        val arg1 = args[1]
        // Both arguments must be collections (arrays or bags) or unknown (null or missing) types
        if ((arg0.code() == PType.ARRAY || arg0.code() == PType.BAG || arg0.code() == PType.UNKNOWN) &&
            (arg1.code() == PType.ARRAY || arg1.code() == PType.BAG || arg1.code() == PType.UNKNOWN)
        ) {
            return getCollectionInstance(arg0, arg1)
        }
        return null
    }

    private fun getCollectionInstance(arg0: PType, arg1: PType): Fn {
        return Function.instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("period1", arg0),
                Parameter("period2", arg1)
            ),
            isNullCall = false,
            isMissingCall = true,
            invoke = { args ->
                val period1 = args[0].toList()
                val period2 = args[1].toList()
                // Each period must have exactly 2 elements (start, end)
                if (period1.size != 2) {
                    throw cardinalityViolationException(period1.size, 2)
                }
                if (period2.size != 2) {
                    throw cardinalityViolationException(period2.size, 2)
                }
                // D1: value of the first field of <row value expression 1>
                val d1 = period1[0]
                val field1 = period1[1]
                // D2: value of the first field of <row value expression 2>
                val d2 = period2[0]
                val field2 = period2[1]
                // For the datetime validation:
                if (!d1.isNull && !d1.isMissing && !isDateTimeType(d1.type)) {
                    throw PErrors.unexpectedTypeException(d1.type, listOf(PType.date(), PType.time(), PType.timestamp()))
                }
                if (!d2.isNull && !d2.isMissing && !isDateTimeType(d2.type)) {
                    throw PErrors.unexpectedTypeException(d2.type, listOf(PType.date(), PType.time(), PType.timestamp()))
                }
                if (!field1.isNull && !field1.isMissing && !isDateTimeType(field1.type) && !isIntervalType(field1.type)) {
                    throw PErrors.unexpectedTypeException(field1.type, listOf(PType.date(), PType.time(), PType.timestamp(), PType.intervalYearMonth(2), PType.intervalDaySecond(2, 6)))
                }
                if (!field2.isNull && !field2.isMissing && !isDateTimeType(field2.type) && !isIntervalType(field2.type)) {
                    throw PErrors.unexpectedTypeException(field2.type, listOf(PType.date(), PType.time(), PType.timestamp(), PType.intervalYearMonth(2), PType.intervalDaySecond(2, 6)))
                }
                // Calculate E1 and E2
                val e1 = calculateEndpoint(d1, field1)
                val e2 = calculateEndpoint(d2, field2)
                // If D1 is null or E1 < D1, then S1 = E1 and T1 = D1. Otherwise, S1 = D1 and T1 = E1.
                val (s1, t1) = if (d1.isNull || d1.isMissing) {
                    Pair(e1, d1)
                } else {
                    val cmp = compareValues(e1, d1)
                    if (cmp != null && !cmp.isNull && !cmp.isMissing && cmp.int < 0) {
                        Pair(e1, d1)
                    } else {
                        Pair(d1, e1)
                    }
                }
                // If D2 is null or E2 < D2, then S2 = E2 and T2 = D2. Otherwise, S2 = D2 and T2 = E2.
                val (s2, t2) = if (d2.isNull || d2.isMissing) {
                    Pair(e2, d2)
                } else {
                    val cmp = compareValues(e2, d2)
                    if (cmp != null && !cmp.isNull && !cmp.isMissing && cmp.int < 0) {
                        Pair(e2, d2)
                    } else {
                        Pair(d2, e2)
                    }
                }
                // Apply SQL-99 OVERLAPS formula:
                // (S1 > S2 AND NOT (S1 >= T2 AND T1 >= T2)) OR
                // (S2 > S1 AND NOT (S2 >= T1 AND T2 >= T1)) OR
                // S1 = S2  (simplified from S1 = S2 AND (T1 <> T2 OR T1 = T2))
                evaluateOverlapsFormula(s1, t1, s2, t2)
            }
        )
    }

    private fun calculateEndpoint(start: Datum, field: Datum): Datum? {
        // If field is null or a datetime, use it directly as E
        if (field.isNull || field.isMissing || isDateTimeType(field.type)) {
            return field
        }
        // If field is an interval, calculate E = D + I
        if (isIntervalType(field.type)) {
            return addInterval(start, field)
        }
        // Should not reach here
        throw PErrors.unexpectedTypeException(field.type, listOf(PType.date(), PType.time(), PType.timestamp(), PType.intervalYearMonth(2), PType.intervalDaySecond(2, 6)))
    }

    private fun addInterval(datetime: Datum, interval: Datum): Datum? {
        return try {
            val plusFn = FnPlus.getInstance(arrayOf(datetime.type, interval.type)) ?: return null
            val result = plusFn.invoke(arrayOf(datetime, interval))
            if (result.isNull) null else result
        } catch (_: Exception) {
            null
        }
    }

    private fun compareValues(a: Datum?, b: Datum?): Datum? {
        // If either value is null or missing, propagate that
        if (a == null || b == null || a.isNull || b.isNull) return Datum.nullValue()
        if (a.isMissing || b.isMissing) return Datum.missing()
        // Validate datetime types are comparable according to SQL-99 spec
        if (isDateTimeType(a.type) && isDateTimeType(b.type)) {
            validateDateTimeComparison(a.type, b.type)
        }
        return Datum.integer(Datum.comparator().compare(a, b))
    }

    private fun validateDateTimeComparison(type1: PType, type2: PType) {
        val code1 = type1.code()
        val code2 = type2.code()
        val compatible = when {
            // DATE types are comparable with DATE types
            code1 == PType.DATE && code2 == PType.DATE -> true
            // TODO(): We currently don't support TIMEZ & TIMESTAMPZ
            // Related Issue: https://github.com/partiql/partiql-lang-kotlin/issues/1795
            // TIME types (with or without timezone) are comparable with each other
            (code1 == PType.TIME || code1 == PType.TIMEZ) && (code2 == PType.TIME || code2 == PType.TIMEZ) -> true
            // TIMESTAMP types (with or without timezone) are comparable with each other
            (code1 == PType.TIMESTAMP || code1 == PType.TIMESTAMPZ) && (code2 == PType.TIMESTAMP || code2 == PType.TIMESTAMPZ) -> true
            else -> false
        }
        if (!compatible) {
            throw PErrors.unexpectedTypeException(type1, listOf(type2))
        }
    }

    private fun evaluateOverlapsFormula(s1: Datum?, t1: Datum?, s2: Datum?, t2: Datum?): Datum {
        val s1GreaterS2 = isGreater(s1, s2)
        val s2GreaterS1 = isGreater(s2, s1)
        val s1EqualsS2 = isEqual(s1, s2)
        val condition1 = logicalAnd(s1GreaterS2, logicalNot(logicalAnd(isGreaterOrEqual(s1, t2), isGreaterOrEqual(t1, t2))))
        val condition2 = logicalAnd(s2GreaterS1, logicalNot(logicalAnd(isGreaterOrEqual(s2, t1), isGreaterOrEqual(t2, t1))))
        return logicalOr(logicalOr(condition1, condition2), s1EqualsS2)
    }

    private fun isGreater(a: Datum?, b: Datum?): Datum {
        val cmp = compareValues(a, b) ?: return Datum.nullValue(PType.bool())
        if (cmp.isNull || cmp.isMissing) return cmp
        return Datum.bool(cmp.int > 0)
    }

    private fun isGreaterOrEqual(a: Datum?, b: Datum?): Datum {
        val cmp = compareValues(a, b) ?: return Datum.nullValue(PType.bool())
        if (cmp.isNull || cmp.isMissing) return cmp
        return Datum.bool(cmp.int >= 0)
    }

    private fun isEqual(a: Datum?, b: Datum?): Datum {
        val cmp = compareValues(a, b) ?: return Datum.nullValue(PType.bool())
        if (cmp.isNull || cmp.isMissing) return cmp
        return Datum.bool(cmp.int == 0)
    }

    // Follow logical operations behavior in FnAnd, FnOr, and FnNot
    private fun logicalAnd(a: Datum, b: Datum): Datum {
        val aIsUnknown = a.isNull || a.isMissing
        val bIsUnknown = b.isNull || b.isMissing
        return when {
            aIsUnknown && bIsUnknown -> Datum.nullValue(PType.bool())
            !aIsUnknown && a.getBoolean() && bIsUnknown -> Datum.nullValue(PType.bool())
            !bIsUnknown && b.getBoolean() && aIsUnknown -> Datum.nullValue(PType.bool())
            !a.getBoolean() || !b.getBoolean() -> Datum.bool(false)
            else -> Datum.bool(true)
        }
    }

    private fun logicalOr(a: Datum, b: Datum): Datum {
        val aIsUnknown = a.isNull || a.isMissing
        val bIsUnknown = b.isNull || b.isMissing
        return when {
            aIsUnknown && bIsUnknown -> Datum.nullValue(PType.bool())
            !aIsUnknown && !bIsUnknown -> Datum.bool(a.getBoolean() || b.getBoolean())
            aIsUnknown && b.getBoolean() -> Datum.bool(true)
            bIsUnknown && a.getBoolean() -> Datum.bool(true)
            else -> Datum.nullValue(PType.bool())
        }
    }

    private fun logicalNot(a: Datum): Datum {
        if (a.isNull || a.isMissing) return Datum.nullValue(PType.bool())
        return Datum.bool(!a.boolean)
    }
}
