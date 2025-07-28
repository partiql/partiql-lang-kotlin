package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function.instance
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
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
        // Both arguments must be collections (arrays or bags)
        if ((arg0.code() == PType.ARRAY || arg0.code() == PType.BAG) &&
            (arg1.code() == PType.ARRAY || arg1.code() == PType.BAG)
        ) {
            return getCollectionInstance(arg0, arg1)
        }
        return null
    }

    private fun getCollectionInstance(arg0: PType, arg1: PType): Fn {
        return instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("period1", arg0),
                Parameter("period2", arg1)
            ),
            invoke = { args ->
                val period1 = args[0].toList()
                val period2 = args[1].toList()
                // Each period must have exactly 2 elements (start, end)
                if (period1.size != 2 || period2.size != 2) {
                    return@instance Datum.nullValue()
                }
                // D1: value of the first field of <row value expression 1>
                val d1 = period1[0]
                val field1 = period1[1]
                // D2: value of the first field of <row value expression 2>
                val d2 = period2[0]
                val field2 = period2[1]
                // For the datetime validation:
                if (!d1.isNull && !isDateTimeType(d1.type)) {
                    throw IllegalArgumentException("OVERLAPS predicate requires datetime start values, but got: ${d1.type}")
                }
                if (!d2.isNull && !isDateTimeType(d2.type)) {
                    throw IllegalArgumentException("OVERLAPS predicate requires datetime start values, but got: ${d2.type}")
                }
                // If D1 or D2 is null, return null
                if (d1.isNull || d2.isNull) {
                    return@instance Datum.nullValue()
                }
                // Calculate E1 and E2
                val e1 = calculateEndpoint(d1, field1) ?: return@instance Datum.nullValue()
                val e2 = calculateEndpoint(d2, field2) ?: return@instance Datum.nullValue()
                // Calculate S1, T1, S2, T2 according to SQL-99 spec
                val (s1, t1) = if (d1.isNull || compareValues(e1, d1) < 0) Pair(e1, d1) else Pair(d1, e1)
                val (s2, t2) = if (d2.isNull || compareValues(e2, d2) < 0) Pair(e2, d2) else Pair(d2, e2)
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
        if (field.isNull) {
            return null
        }
        // If field is a datetime, use it directly as E
        if (isDateTimeType(field.type)) {
            return field
        }
        // If field is an interval, calculate E = D + I
        if (isIntervalType(field.type)) {
            if (start.isNull) {
                return null
            }
            return addInterval(start, field)
        }
        // Throw error for invalid field types
        throw IllegalArgumentException("OVERLAPS predicate requires datetime or interval types, but got: ${field.type}")
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

    private fun compareValues(a: Datum, b: Datum): Int = Datum.comparator().compare(a, b)
}
