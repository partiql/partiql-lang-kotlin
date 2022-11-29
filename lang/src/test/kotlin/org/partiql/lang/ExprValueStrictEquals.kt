package org.partiql.lang

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.bigDecimalValue
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.bytesValue
import org.partiql.lang.eval.dateValue
import org.partiql.lang.eval.intValue
import org.partiql.lang.eval.name
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.timeValue
import org.partiql.lang.eval.timestampValue

/**
 * Used to verify the actual `ExprValue` is the same as the expected `ExprValue` in test
 *
 * Ensures 2 [ExprValue]s to have the same type & value
 */
fun ExprValue.strictEquals(other: ExprValue): Boolean =
    ExprValueStrictComparator.compare(this, other) == 0

private object ExprValueStrictComparator : Comparator<ExprValue> {
    override fun compare(v1: ExprValue, v2: ExprValue): Int {
        if (v1.type !== v2.type) {
            return v1.type.compareTo(v2.type)
        }

        return when (v1.type) {
            ExprValueType.MISSING -> 0
            ExprValueType.NULL -> v1.ionValue.type.compareTo(v2.ionValue.type)
            ExprValueType.BOOL -> v1.booleanValue().compareTo(v2.booleanValue())
            ExprValueType.INT -> v1.intValue().compareTo(v2.intValue())
            ExprValueType.FLOAT -> v1.numberValue().toDouble().compareTo(v2.numberValue().toDouble())
            ExprValueType.DECIMAL -> v1.bigDecimalValue().compareTo(v2.bigDecimalValue())
            ExprValueType.DATE -> v1.dateValue().compareTo(v2.dateValue())
            ExprValueType.TIMESTAMP -> v1.timestampValue().compareTo(v2.timestampValue())
            ExprValueType.TIME -> {
                val (localTime1, precision1, zoneOffset1) = v1.timeValue()
                val (localTime2, precision2, zoneOffset2) = v2.timeValue()

                val localTimeCompareResult = localTime1.compareTo(localTime2)
                if (localTimeCompareResult != 0) {
                    return localTimeCompareResult
                }

                val precisionCompareResult = precision1.compareTo(precision2)
                if (precisionCompareResult != 0) {
                    return precisionCompareResult
                }

                return when {
                    zoneOffset1 == null && zoneOffset2 == null -> 0
                    zoneOffset1 == null -> 1
                    zoneOffset2 == null -> -1
                    else -> zoneOffset1.compareTo(zoneOffset2)
                }
            }
            ExprValueType.SYMBOL,
            ExprValueType.STRING -> v1.stringValue().compareTo(v2.stringValue())
            ExprValueType.CLOB,
            ExprValueType.BLOB -> {
                val bytes1 = v1.bytesValue()
                val bytes2 = v2.bytesValue()

                val lengthCompareResult = bytes1.size.compareTo(bytes2.size)
                if (lengthCompareResult != 0) {
                    return lengthCompareResult
                }

                v1.bytesValue().sumOf { it.hashCode() }.compareTo(v2.bytesValue().sumOf { it.hashCode() })
            }
            ExprValueType.LIST,
            ExprValueType.SEXP -> compareOrderedElements(v1.iterator(), v2.iterator())
            ExprValueType.BAG -> {
                val v11 = v1.sortedWith(this).iterator()
                val v22 = v2.sortedWith(this).iterator()

                compareOrderedElements(
                    v11,
                    v22
                )
            }
            ExprValueType.STRUCT -> compareOrderedStruct(
                v1.sortedWith(namedValueComparator).iterator(),
                v2.sortedWith(namedValueComparator).iterator()
            )
        }
    }

    fun compareOrderedStruct(it1: Iterator<ExprValue>, it2: Iterator<ExprValue>): Int {
        while (it1.hasNext()) {
            if (!it2.hasNext()) {
                return -1
            }
            val item1 = it1.next()
            val item2 = it2.next()
            val compareResult = namedValueComparator.compare(item1, item2)
            if (compareResult != 0) {
                return compareResult
            }
        }
        return when {
            it2.hasNext() -> 1
            else -> 0
        }
    }

    val namedValueComparator = NameValueComparator()

    class NameValueComparator : Comparator<ExprValue> {
        override fun compare(v1: ExprValue, v2: ExprValue): Int {
            val name1 = v1.name ?: error("[NamedExprValueComparator] expects a named [ExprValue]. Actually got: $v1")
            val name2 = v2.name ?: error("[NamedExprValueComparator] expects a named [ExprValue]. Actually got: $v2")

            return when (val nameCompareResult = name1.stringValue().compareTo(name2.stringValue())) {
                0 -> ExprValueStrictComparator.compare(v1, v2)
                else -> nameCompareResult
            }
        }
    }

    fun compareOrderedElements(it1: Iterator<ExprValue>, it2: Iterator<ExprValue>): Int {
        while (it1.hasNext()) {
            if (!it2.hasNext()) {
                return -1
            }
            val item1 = it1.next()
            val item2 = it2.next()
            val compareResult = compare(item1, item2)
            if (compareResult != 0) {
                return compareResult
            }
        }
        return when {
            it2.hasNext() -> 1
            else -> 0
        }
    }
}
