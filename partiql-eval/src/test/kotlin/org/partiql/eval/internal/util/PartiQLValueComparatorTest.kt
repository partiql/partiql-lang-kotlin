package org.partiql.eval.internal.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.value.BagValue
import org.partiql.value.ListValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.bagValue
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.clobValue
import org.partiql.value.dateValue
import org.partiql.value.datetime.DateTimeValue.date
import org.partiql.value.datetime.DateTimeValue.time
import org.partiql.value.datetime.DateTimeValue.timestamp
import org.partiql.value.datetime.TimeZone
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.listValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.sexpValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.symbolValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue
import org.partiql.value.toIon
import java.math.BigDecimal
import java.util.Base64
import java.util.Random

@OptIn(PartiQLValueExperimental::class)
class PartiQLValueComparatorTest {
    class EquivValues(vararg val values: PartiQLValue)

    private val nullsFirstComparator = PartiQLValueComparator(nullOrder = PartiQLValueComparator.NullOrder.FIRST)
    private val nullsLastComparator = PartiQLValueComparator(nullOrder = PartiQLValueComparator.NullOrder.LAST)

    // TODO consider replacing linear congruential generator with something else (e.g. xorshift)
    // RNG for fuzz testing the sort orders, the seed is arbitrary but static for determinism
    private val SEED = 0x59CF3400BEF36A67

    private val emptyList: ListValue<PartiQLValue> = listValue(emptyList())
    private val emptyBag: BagValue<PartiQLValue> = bagValue(emptyList())
    private fun emptyStruct(annotations: List<String> = emptyList()): StructValue<PartiQLValue> = structValue(annotations = annotations)

    private fun base64Decode(s: String): ByteArray = Base64.getDecoder().decode(s)

    // Checks that [allValues], when shuffled and sorted using [comparator], follow same ordering as [allValues]
    private fun checkAllEquivalent(allValues: List<PartiQLValue>, comparator: PartiQLValueComparator) {
        val shuffledValues = allValues.shuffled(Random(SEED))
        val sortedAfterShuffle = shuffledValues.sortedWith(comparator)
        assertEquals(allValues.size, sortedAfterShuffle.size)
        allValues.zip(sortedAfterShuffle)
            .forEach {
                assertEquals(0, comparator.compare(it.first, it.second), "${it.first.toIon()} != ${it.second.toIon()}")
            }
    }

    @Test
    fun testNullsFirst() {
        val sortedValsNullsFirst = (nullValues + nonNullPartiQLValue).flatMap {
            it.values.asIterable()
        }
        checkAllEquivalent(sortedValsNullsFirst, nullsFirstComparator)
    }

    @Test
    fun testNullsLast() {
        val sortedValsNullsLast = (nonNullPartiQLValue + nullValues).flatMap {
            it.values.asIterable()
        }
        checkAllEquivalent(sortedValsNullsLast, nullsLastComparator)
    }

    @Test
    fun checkEquivalenceClasses() {
        // Checks that all the values in an [EquivValues] are equivalent using both comparators
        (nullValues + nonNullPartiQLValue).forEach {
            val values = it.values
            values.forEach { v1 ->
                values.forEach { v2 ->
                    assertEquals(0, nullsFirstComparator.compare(v1, v2), "${v1.toIon()} != ${v1.toIon()}")
                    assertEquals(0, nullsLastComparator.compare(v1, v2), "${v1.toIon()} != ${v1.toIon()}")
                }
            }
        }
    }

    private val nullValues = listOf(
        EquivValues(
            nullValue(), // null
            missingValue(), // missing
            nullValue(annotations = listOf("a")), // `a::null`
            missingValue(annotations = listOf("a")), // `a::missing`
            int32Value(null), // `null.int`,
            structValue<PartiQLValue>(null) // `null.struct`
        )
    )

    private val nonNullPartiQLValue = listOf(
        EquivValues(
            boolValue(false),
            boolValue(false, annotations = listOf("b"))
        ),
        EquivValues(
            boolValue(true, annotations = listOf("c")),
            boolValue(true)
        ),
        EquivValues(
            // make sure there are at least two nan
            float32Value(Float.NaN),
            float64Value(Double.NaN),
        ),
        EquivValues(
            // make sure there are at least two nan
            float32Value(Float.NEGATIVE_INFINITY),
            float64Value(Double.NEGATIVE_INFINITY),
        ),
        EquivValues(
            float32Value(-1e1000f),
            float64Value(-1e1000)
        ),
        EquivValues(
            float32Value(-5e-1f),
            float64Value(-5e-1),
            decimalValue(BigDecimal("-0.50000000000000000000000000")),
            float32Value(-0.5e0f),
            float64Value(-0.5e0)
        ),
        EquivValues(
            decimalValue(BigDecimal("-0.0")),
            decimalValue(BigDecimal("-0.0000000000")),
            float32Value(0e0f),
            float64Value(0e0),
            float32Value(-0e0f),
            float64Value(-0e0),
            decimalValue(BigDecimal("0e10000")),
            int32Value(0),
            int32Value(-0),
            int64Value(0),
            int64Value(-0)
        ),
        EquivValues(
            float32Value(5e9f),
            float64Value(5e9),
            // 5000000000 does not fit into int32
            int64Value(5000000000),
            int64Value(0x12a05f200),
            float32Value(5.0e9f),
            float64Value(5.0e9),
            decimalValue(BigDecimal("5e9")),
            decimalValue(BigDecimal("5.00000e9")),
        ),
        EquivValues(
            // make sure there are at least two +inf
            float32Value(Float.POSITIVE_INFINITY),
            float64Value(Double.POSITIVE_INFINITY),
        ),
        EquivValues(
            dateValue(date(year = 1992, month = 8, day = 22))
        ),
        EquivValues(
            dateValue(date(year = 2021, month = 8, day = 22))
        ),
        EquivValues(
            timeValue(time(hour = 12, minute = 12, second = 12, timeZone = TimeZone.UnknownTimeZone)),
            timeValue(time(hour = 12, minute = 12, second = 12, nano = 0, timeZone = TimeZone.UnknownTimeZone)),
            timeValue(time(hour = 12, minute = 12, second = 12, timeZone = TimeZone.UnknownTimeZone)),
            // time second precision handled by time constructor
            timeValue(time(hour = 12, minute = 12, second = 12, timeZone = TimeZone.UtcOffset.of(0))),
        ),
        EquivValues(
            timeValue(time(hour = 12, minute = 12, second = 12, nano = 100000000, timeZone = TimeZone.UnknownTimeZone)),
        ),
        EquivValues(
            timeValue(time(hour = 12, minute = 12, second = 12, nano = 0, timeZone = TimeZone.UtcOffset.of(-8, 0))),
            timeValue(time(hour = 12, minute = 12, second = 12, timeZone = TimeZone.UtcOffset.of(-8, 0))),
        ),
        EquivValues(
            timeValue(time(hour = 12, minute = 12, second = 12, nano = 100000000, timeZone = TimeZone.UtcOffset.of(-9, 0))),
        ),
        EquivValues(
            timestampValue(timestamp(year = 2017, timeZone = TimeZone.UtcOffset.of(0, 0))), // `2017T`
            timestampValue(timestamp(year = 2017, month = 1, timeZone = TimeZone.UtcOffset.of(0, 0))), // `2017-01T`
            timestampValue(timestamp(year = 2017, month = 1, day = 1, timeZone = TimeZone.UtcOffset.of(0, 0))), // `2017-01-01T`
            timestampValue(timestamp(year = 2017, month = 1, day = 1, hour = 0, minute = 0, second = 0, timeZone = TimeZone.UtcOffset.of(0, 0))), // `2017-01-01T00:00-00:00`
            timestampValue(timestamp(year = 2017, month = 1, day = 1, hour = 1, minute = 0, second = 0, timeZone = TimeZone.UtcOffset.of(1, 0))) // `2017-01-01T01:00+01:00`
        ),
        EquivValues(
            timestampValue(timestamp(year = 2017, month = 1, day = 1, hour = 1, minute = 0, second = 0, timeZone = TimeZone.UtcOffset.of(0, 0))) // `2017-01-01T01:00Z`
        ),
        EquivValues(
            stringValue(value = ""),
            stringValue(value = "", annotations = listOf("foobar")),
            symbolValue(value = ""),
            symbolValue(value = "", annotations = listOf("foobar"))
        ),
        EquivValues(
            stringValue(value = "A"),
            stringValue(value = "A", annotations = listOf("foobar")),
            symbolValue(value = "A"),
            symbolValue(value = "A", annotations = listOf("foobar"))
        ),
        EquivValues(
            stringValue(value = "AA"),
            symbolValue(value = "AA"),
        ),
        EquivValues(
            stringValue(value = "a"),
            symbolValue(value = "a"),
        ),
        EquivValues(
            stringValue(value = "azzzzzzz"),
            symbolValue(value = "azzzzzzz"),
        ),
        EquivValues(
            stringValue(value = "z"),
            symbolValue(value = "z"),
        ),
        // TODO add a UTF-16 order breaker here to verify we're doing the right thing
        EquivValues(
            stringValue(value = "\uD83D\uDCA9"),
            symbolValue(value = "\uD83D\uDCA9"),
        ),
        EquivValues(
            blobValue(base64Decode("")), // `{{}}`
            clobValue("".toByteArray()) // `{{\"\"}}`
        ),
        EquivValues(
            blobValue(base64Decode("QQ==")), // `{{QQ==}}`
            clobValue("A".toByteArray()) //  `{{\"A\"}}`
        ),
        EquivValues(
            blobValue(base64Decode("YWFhYWFhYWFhYWFhYQ==")), // `{{YWFhYWFhYWFhYWFhYQ==}}`
            clobValue("aaaaaaaaaaaaa".toByteArray()) // `{{"aaaaaaaaaaaaa"}}`
        ),
        EquivValues(
            emptyList, // []
            listValue(emptyList(), annotations = listOf("z", "x", "y")) // `z::x::y::[]`
        ),
        EquivValues(
            listValue(boolValue(false), emptyStruct()) // [false, {}]
        ),
        EquivValues(
            listValue(boolValue(true)) // [true]
        ),
        EquivValues(
            listValue(boolValue(true), boolValue(true)) // [true, true]
        ),
        EquivValues(
            listValue(boolValue(true), int32Value(100)) // [true, 100]
        ),
        EquivValues(
            listValue(listOf(listValue(int32Value(1)))) // [[1]]
        ),
        EquivValues(
            listValue(listOf(listValue(int32Value(1), int32Value(1)))) // [[1, 1]]
        ),
        EquivValues(
            listValue(listOf(listValue(int32Value(1), int32Value(2)))) // [[1, 2]]
        ),
        EquivValues(
            listValue(listOf(listValue(int32Value(2), int32Value(1)))) // [[2, 1]]
        ),
        EquivValues(
            listValue(listOf(listValue(listOf(listValue(int32Value(1)))))) // [[[1]]]
        ),
        EquivValues(
            sexpValue(emptyList(), annotations = listOf("a", "b", "c")) // `a::b::c::()`
        ),
        EquivValues(
            sexpValue(float32Value(1f)), // "`a::b::c::(1e0)`"
            sexpValue(float64Value(1.0), annotations = listOf("a", "b", "c")), // "`a::b::c::(1e0)`"
            sexpValue(int32Value(1)), // `(1)`
            sexpValue(decimalValue(BigDecimal("1.0000000000000"))) // `(1.0000000000000)`
        ),
        EquivValues(
            sexpValue(timestampValue(timestamp(year = 2012)), float32Value(Float.NaN)) // `(2012T nan)`
        ),
        EquivValues(
            sexpValue(timestampValue(timestamp(year = 2012)), int32Value(1), int32Value(2), int32Value(3)) // `(2012T 1 2 3)`
        ),
        EquivValues(
            sexpValue(listOf(listValue(emptyList()))) // `([])`
        ),
        EquivValues(
            sexpValue(emptyList, emptyList) // `([] [])`
        ),
        EquivValues(
            emptyStruct(), // {}
            emptyStruct(annotations = listOf("m", "n", "o")) // `m::n::o::{}`
        ),
        EquivValues(
            structValue( // {'a': true, 'b': 1000, 'c': false}
                "a" to boolValue(true), "b" to int32Value(1000), "c" to boolValue(false)
            ),
            structValue( // {'b': `1e3`, 'a': true, 'c': false}
                "b" to float32Value(1000f), "a" to boolValue(true), "c" to boolValue(false)
            )
        ),
        EquivValues(
            structValue( // {'b': 1000, 'c': false}
                "b" to int32Value(1000), "c" to boolValue(false)
            ),
            structValue( // {'c': false, 'b': 1.00000000e3}
                "c" to boolValue(false), "b" to decimalValue(BigDecimal("1.00000000e3"))
            )
        ),
        EquivValues(
            structValue( // {'c': false}
                "c" to boolValue(false)
            )
        ),
        EquivValues(
            structValue( // {'d': 1, 'f': 2}
                "d" to int32Value(1), "f" to int32Value(2)
            )
        ),
        EquivValues(
            structValue( // {'d': 2, 'e': 3, 'f': 4}
                "d" to int32Value(2),
                "e" to int32Value(3),
                "f" to int32Value(4)
            )
        ),
        EquivValues(
            structValue( // {'d': 3, 'e': 2}
                "d" to int32Value(3),
                "e" to int32Value(2)
            )
        ),
        EquivValues(
            structValue( // { 'm': [1, 1], 'n': [1, 1]}
                "m" to listValue(int32Value(1), int32Value(1)),
                "n" to listValue(int32Value(1), int32Value(1))
            )
        ),
        EquivValues(
            structValue( // { 'm': [1, 1], 'n': [1, 2]}
                "m" to listValue(int32Value(1), int32Value(1)),
                "n" to listValue(int32Value(1), int32Value(2))
            )
        ),
        EquivValues(
            structValue( // { 'm': [1, 1], 'n': [2, 2]}
                "m" to listValue(int32Value(1), int32Value(1)),
                "n" to listValue(int32Value(2), int32Value(2))
            )
        ),
        EquivValues(
            structValue( // { 'm': [1, 2], 'n': [2, 2]}
                "m" to listValue(int32Value(1), int32Value(2)),
                "n" to listValue(int32Value(2), int32Value(2))
            )
        ),
        EquivValues(
            structValue( // { 'm': [2, 2], 'n': [2, 2]}
                "m" to listValue(int32Value(1), int32Value(2)),
                "n" to listValue(int32Value(2), int32Value(2))
            )
        ),
        EquivValues(
            structValue( // { 'm': <<1, 1>>, 'n': []}
                "m" to bagValue(int32Value(1), int32Value(1)),
                "n" to emptyList
            )
        ),
        EquivValues(
            structValue( // { 'm': <<1, 1>>, 'n': <<>>}
                "m" to bagValue(int32Value(1), int32Value(1)),
                "n" to emptyBag
            )
        ),
        EquivValues( // {'x': 1, 'y': 2}
            structValue(
                "x" to int32Value(1),
                "y" to int32Value(2)
            )
        ),
        EquivValues( // {'x': 1, 'y': 2, 'z': 1}
            structValue(
                "x" to int32Value(1),
                "y" to int32Value(2),
                "z" to int32Value(1)
            )
        ),
        EquivValues( // <<>>
            emptyBag
        ),
        EquivValues(
            // The ordered values are: true, true, 1
            // <<1, true, true>>
            bagValue(int32Value(1), boolValue(true), boolValue(true))
        ),
        EquivValues(
            // The ordered values are: true, true, 1, 1, 1
            // <<true, 1, 1.0, `1e0`, true>>
            bagValue(boolValue(true), int32Value(1), decimalValue(BigDecimal("1.0")), float32Value(1e0f), boolValue(true))
        ),
        EquivValues( // <<1>>
            bagValue(int32Value(1))
        ),
        EquivValues( // <<1, 1>>
            bagValue(int32Value(1), int32Value(1))
        ),
        EquivValues( // << [] >>
            bagValue(listOf(emptyList))
        ),
        EquivValues( // << {}, [] >>
            bagValue(emptyStruct(), emptyList)
        ),
        EquivValues( // << {} >>
            bagValue(emptyStruct())
        ),
        EquivValues( // << <<>> >>
            bagValue(listOf(emptyBag))
        ),
        EquivValues( // << <<>>, <<>> >>
            bagValue(emptyBag, emptyBag)
        )
    )
}
