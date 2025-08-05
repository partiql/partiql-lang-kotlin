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
import org.partiql.spi.utils.FunctionUtils.isUnknown
import org.partiql.spi.utils.FunctionUtils.logicalAnd
import org.partiql.spi.utils.FunctionUtils.logicalNot
import org.partiql.spi.utils.FunctionUtils.logicalOr
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
    private val datetimeTypes = listOf(PType.date(), PType.time(), PType.timez(), PType.timestamp(), PType.timestampz())
    private val fieldTypes = listOf(PType.date(), PType.time(), PType.timez(), PType.timestamp(), PType.timestampz(), PType.intervalYearMonth(2), PType.intervalDaySecond(2, 6))
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
            isNullCall = true,
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
                if (!isUnknown(d1) && !isDateTimeType(d1.type)) {
                    throw PErrors.unexpectedTypeException(d1.type, datetimeTypes)
                }
                if (!isUnknown(d2) && !isDateTimeType(d2.type)) {
                    throw PErrors.unexpectedTypeException(d2.type, datetimeTypes)
                }
                if (!isUnknown(field1) && !isDateTimeType(field1.type) && !isIntervalType(field1.type)) {
                    throw PErrors.unexpectedTypeException(field1.type, fieldTypes)
                }
                if (!isUnknown(field2) && !isDateTimeType(field2.type) && !isIntervalType(field2.type)) {
                    throw PErrors.unexpectedTypeException(field2.type, fieldTypes)
                }
                // Calculate E1 and E2
                val e1 = calculateEndpoint(d1, field1)
                val e2 = calculateEndpoint(d2, field2)
                val (s1, t1) = calculateStartEnd(d1, e1)
                val (s2, t2) = calculateStartEnd(d2, e2)
                // Apply SQL-99 OVERLAPS formula:
                // (S1 > S2 AND NOT (S1 >= T2 AND T1 >= T2)) OR
                // (S2 > S1 AND NOT (S2 >= T1 AND T2 >= T1)) OR
                // S1 = S2  (simplified from S1 = S2 AND (T1 <> T2 OR T1 = T2))
                evaluateOverlapsFormula(s1, t1, s2, t2)
            }
        )
    }

    private fun calculateEndpoint(start: Datum, field: Datum): Datum {
        // If field is null or a datetime, use it directly as E
        if (isUnknown(field) || isDateTimeType(field.type)) {
            return field
        }
        // If field is an interval, calculate E = D + I
        if (isIntervalType(field.type)) {
            return addInterval(start, field)
        }
        // Should not reach here
        throw PErrors.unexpectedTypeException(field.type, fieldTypes)
    }

    private fun addInterval(datetime: Datum, interval: Datum): Datum {
        val plusFn = FnPlus.getInstance(arrayOf(datetime.type, interval.type))
            ?: throw PErrors.unexpectedTypeException(interval.type, listOf(PType.intervalYearMonth(2), PType.intervalDaySecond(2, 6)))
        return plusFn.invoke(arrayOf(datetime, interval))
    }

    private fun compareValues(a: Datum, b: Datum): Datum {
        // If either value is null or missing, propagate that
        if (a.isNull || b.isNull) return Datum.nullValue()
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

    private fun evaluateOverlapsFormula(s1: Datum, t1: Datum, s2: Datum, t2: Datum): Datum {
        val s1GreaterS2 = isGreater(s1, s2)
        val s2GreaterS1 = isGreater(s2, s1)
        val s1EqualsS2 = isEqual(s1, s2)
        val condition1 = logicalAnd(s1GreaterS2, logicalNot(logicalAnd(isGreaterOrEqual(s1, t2), isGreaterOrEqual(t1, t2))))
        val condition2 = logicalAnd(s2GreaterS1, logicalNot(logicalAnd(isGreaterOrEqual(s2, t1), isGreaterOrEqual(t2, t1))))
        return logicalOr(logicalOr(condition1, condition2), s1EqualsS2)
    }

    private fun isGreater(a: Datum, b: Datum): Datum {
        val cmp = compareValues(a, b)
        if (isUnknown(cmp)) return cmp
        return Datum.bool(cmp.int > 0)
    }

    private fun isGreaterOrEqual(a: Datum, b: Datum): Datum {
        val cmp = compareValues(a, b)
        if (isUnknown(cmp)) return cmp
        return Datum.bool(cmp.int >= 0)
    }

    private fun calculateStartEnd(d: Datum, e: Datum): Pair<Datum, Datum> {
        return if (isUnknown(d)) {
            Pair(e, d)
        } else {
            val cmp = compareValues(e, d)
            if (!isUnknown(cmp) && cmp.int < 0) {
                Pair(e, d)
            } else {
                Pair(d, e)
            }
        }
    }

    private fun isEqual(a: Datum, b: Datum): Datum {
        val cmp = compareValues(a, b)
        if (isUnknown(cmp)) return cmp
        return Datum.bool(cmp.int == 0)
    }
}
