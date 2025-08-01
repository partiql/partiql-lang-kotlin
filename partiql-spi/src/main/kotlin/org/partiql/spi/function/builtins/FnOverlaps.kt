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
        // Both arguments must be collections (arrays or bags) or dynamic/unknown types
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
                // If D1 or D2 is null, return null
                if (d1.isNull || d2.isNull) {
                    return@instance Datum.nullValue()
                }
                // If D1 or D2 is missing, propagate missing
                if (d1.isMissing || d2.isMissing) {
                    return@instance Datum.missing()
                }
                // Calculate E1 and E2
                val e1 = calculateEndpoint(d1, field1) ?: return@instance Datum.nullValue()
                val e2 = calculateEndpoint(d2, field2) ?: return@instance Datum.nullValue()
                // Calculate S1, T1, S2, T2 according to SQL-99 spec
                val (s1, t1) = if (compareValues(e1, d1) < 0) Pair(e1, d1) else Pair(d1, e1)
                val (s2, t2) = if (compareValues(e2, d2) < 0) Pair(e2, d2) else Pair(d2, e2)
                // Apply SQL-99 OVERLAPS formula:
                // (S1 > S2 AND NOT (S1 >= T2 AND T1 >= T2)) OR
                // (S2 > S1 AND NOT (S2 >= T1 AND T2 >= T1)) OR
                // ( S1 = S2 AND ( T1 <> T2 OR T1 = T2 ) ) -> Simply to ( S1 = S2 )
                val result = (compareValues(s1, s2) > 0 && !(compareValues(s1, t2) >= 0 && compareValues(t1, t2) >= 0)) ||
                    (compareValues(s2, s1) > 0 && !(compareValues(s2, t1) >= 0 && compareValues(t2, t1) >= 0)) ||
                    (compareValues(s1, s2) == 0)
                Datum.bool(result)
            }
        )
    }

    private fun calculateEndpoint(start: Datum, field: Datum): Datum? {
        if (field.isNull || field.isMissing) {
            return null
        }
        // If field is a datetime, use it directly as E
        if (isDateTimeType(field.type)) {
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
        } catch (e: Exception) {
            null
        }
    }

    private fun compareValues(a: Datum, b: Datum): Int {
        // Validate datetime types are comparable according to SQL-99 spec
        if (isDateTimeType(a.type) && isDateTimeType(b.type)) {
            validateDateTimeComparison(a.type, b.type)
        }
        return Datum.comparator().compare(a, b)
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
}
