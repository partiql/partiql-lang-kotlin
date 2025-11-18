package org.partiql.spi.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.Random

/**
 * TODO: Add support for annotations on Datum
 */
class DatumComparatorTest {
    class EquivValues(vararg val values: Datum)

    private val nullsFirstComparator = Datum.comparator()
    private val nullsLastComparator = Datum.comparator(false)

    // TODO consider replacing linear congruential generator with something else (e.g. xorshift)
    // RNG for fuzz testing the sort orders, the seed is arbitrary but static for determinism
    private val SEED = 0x59CF3400BEF36A67

    private val emptyList: Datum = Datum.array(emptyList())
    private val emptyBag: Datum = Datum.bag(emptyList())
    private fun emptyStruct(): Datum = Datum.struct(emptyList())

    private fun base64Decode(s: String): ByteArray = Base64.getDecoder().decode(s)

    // Checks that [allValues], when shuffled and sorted using [comparator], follow same ordering as [allValues]
    private fun checkAllEquivalent(allValues: List<Datum>, comparator: Comparator<Datum>) {
        val shuffledValues = allValues.shuffled(Random(SEED))
        val sortedAfterShuffle = shuffledValues.sortedWith(comparator)
        assertEquals(allValues.size, sortedAfterShuffle.size)
        allValues.zip(sortedAfterShuffle)
            .forEach {
                assert(0 == comparator.compare(it.first, it.second)) {
                    buildString {
                        appendLine("COMPARISON FAILURE: ${it.first} != ${it.second}")
                        appendLine("FULL LIST:")
                        for (i in 0 until allValues.size) {
                            val allValuesElement = allValues[i]
                            val sortedElement = sortedAfterShuffle[i]
                            append(i)
                            append(": ")
                            when (comparator.compare(allValuesElement, sortedElement)) {
                                0 -> appendLine("TRUE")
                                else -> appendLine("FALSE")
                            }
                            append('\t')
                            append("EXPECTED :")
                            appendLine(allValuesElement)
                            append('\t')
                            append("ACTUAL   :")
                            appendLine(sortedElement)
                        }
                    }
                }
            }
    }

    @Test
    fun testNullsFirst() {
        val sortedValsNullsFirst = (nullValues + nonNullDatum).flatMap {
            it.values.asIterable()
        }
        checkAllEquivalent(sortedValsNullsFirst, nullsFirstComparator)
    }

    @Test
    fun testNullsLast() {
        val sortedValsNullsLast = (nonNullDatum + nullValues).flatMap {
            it.values.asIterable()
        }
        checkAllEquivalent(sortedValsNullsLast, nullsLastComparator)
    }

    @Test
    fun checkEquivalenceClasses() {
        // Checks that all the values in an [EquivValues] are equivalent using both comparators
        (nullValues + nonNullDatum).forEach {
            val values = it.values
            values.forEach { v1 ->
                values.forEach { v2 ->
                    assertEquals(0, nullsFirstComparator.compare(v1, v2), "$v1 != $v2")
                    assertEquals(0, nullsLastComparator.compare(v1, v2), "$v1 != $v2")
                }
            }
        }
    }

    // [EquivValues] in this list are sorted from ascending order per the less-than-order-by operator defined in spec
    // section 12.2. Values within each [EquivValues] are equivalent.
    private val nullValues = listOf(
        EquivValues(
            Datum.nullValue(), // null
            Datum.missing(), // missing
            Datum.nullValue(), // TODO: annotations = listOf("a")), // `a::null`
            Datum.missing(), // TODO: annotations = listOf("a")), // `a::missing`
            Datum.nullValue(PType.numeric(38, 0)), // `null.int`,
            Datum.nullValue(PType.struct()) // `null.struct`
        )
    )

    private val nonNullDatum = listOf(
        EquivValues(
            Datum.bool(false), // false
            Datum.bool(false), // TODO: annotations = listOf("b")) // `b::false`
        ),
        EquivValues(
            Datum.bool(true), // TODO: annotations = listOf("c")), // `c::true`
            Datum.bool(true) // true
        ),
        EquivValues(
            // make sure there are at least two nan
            Datum.real(Float.NaN),
            Datum.doublePrecision(Double.NaN),
        ),
        EquivValues(
            // make sure there are at least two -inf
            Datum.real(Float.NEGATIVE_INFINITY),
            Datum.doublePrecision(Double.NEGATIVE_INFINITY),
        ),
        EquivValues(
            Datum.real(-1e1000f), // -inf
            Datum.doublePrecision(-1e1000) // -inf
        ),
        EquivValues(
            Datum.real(-5e-1f),
            Datum.doublePrecision(-5e-1),
            Datum.decimal(BigDecimal("-0.50000000000000000000000000")),
            Datum.real(-0.5e0f),
            Datum.doublePrecision(-0.5e0)
        ),
        EquivValues(
            Datum.decimal(BigDecimal("-0.0")),
            Datum.decimal(BigDecimal("-0.0000000000")),
            Datum.real(0e0f),
            Datum.doublePrecision(0e0),
            Datum.real(-0e0f),
            Datum.doublePrecision(-0e0),
            Datum.decimal(BigDecimal("0e10000")),
            Datum.integer(0),
            Datum.integer(-0),
            Datum.bigint(0),
            Datum.bigint(-0)
        ),
        EquivValues(
            Datum.real(5e9f),
            Datum.doublePrecision(5e9),
            // 5000000000 does not fit into int32
            Datum.bigint(5000000000),
            Datum.bigint(0x12a05f200),
            Datum.real(5.0e9f),
            Datum.doublePrecision(5.0e9),
            Datum.decimal(BigDecimal("5e9")),
            Datum.decimal(BigDecimal("5.00000e9")),
        ),
        EquivValues(
            // make sure there are at least two +inf
            Datum.real(Float.POSITIVE_INFINITY),
            Datum.doublePrecision(Double.POSITIVE_INFINITY),
        ),
        EquivValues(
            Datum.timez(OffsetTime.of(12, 12, 12, 100000000, ZoneOffset.ofHours(1)), 9),
        ),
        EquivValues(
            Datum.time(LocalTime.of(12, 12, 12, 100000000), 9),
            Datum.timez(OffsetTime.of(12, 12, 12, 100000000, ZoneOffset.UTC), 9),
        ),
        EquivValues(
            Datum.timez(OffsetTime.of(12, 12, 12, 100000000, ZoneOffset.ofHours(-1)), 9),
        ),
        EquivValues(
            Datum.date(LocalDate.of(1992, 8, 22))
        ),
        EquivValues(
            Datum.timestampz(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1)), 6), // `2017-01-01T00:00+01:00`
        ),
        EquivValues(
            Datum.timestamp(LocalDateTime.of(2017, 1, 1, 0, 0, 0), 6),
            Datum.timestampz(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0)), 6), // `2017-01-01T00:00+00`
        ),
        EquivValues(
            Datum.timestampz(OffsetDateTime.of(2017, 1, 1, 1, 0, 0, 0, ZoneOffset.ofHours(-1)), 6), // `2017-01-01T01:00-01:00`
        ),
        EquivValues(
            Datum.date(LocalDate.of(2021, 8, 22))
        ),
        EquivValues(
            Datum.string(""),
            // TODO: Datum.string("", annotations = listOf("foobar")),
        ),
        EquivValues(
            Datum.string("A"),
            // TODO: Datum.string("A", annotations = listOf("foobar")),
        ),
        EquivValues(
            Datum.string("AA"),
        ),
        EquivValues(
            Datum.string("a"),
        ),
        EquivValues(
            Datum.string("azzzzzzz"),
        ),
        EquivValues(
            Datum.string("z"),
        ),
        // TODO add a UTF-16 order breaker here to verify we're doing the right thing
        EquivValues(
            Datum.string("\uD83D\uDCA9"),
        ),
        EquivValues(
            Datum.blob(base64Decode("")), // `{{}}`
            Datum.clob("".toByteArray()) // `{{\"\"}}`
        ),
        EquivValues(
            Datum.blob(base64Decode("QQ==")), // `{{QQ==}}`
            Datum.clob("A".toByteArray()) //  `{{\"A\"}}`
        ),
        EquivValues(
            Datum.blob(base64Decode("YWFhYWFhYWFhYWFhYQ==")), // `{{YWFhYWFhYWFhYWFhYQ==}}`
            Datum.clob("aaaaaaaaaaaaa".toByteArray()) // `{{"aaaaaaaaaaaaa"}}`
        ),
        EquivValues(
            emptyList, // []
            // TODO: Datum.list(emptyList(), annotations = listOf("z", "x", "y")) // `z::x::y::[]`
        ),
        EquivValues(
            Datum.array(listOf(Datum.bool(false), emptyStruct())) // [false, {}]
        ),
        EquivValues(
            Datum.array(listOf(Datum.bool(true))) // [true]
        ),
        EquivValues(
            Datum.array(listOf(Datum.bool(true), Datum.bool(true))) // [true, true]
        ),
        EquivValues(
            Datum.array(listOf(Datum.bool(true), Datum.integer(100))) // [true, 100]
        ),
        EquivValues(
            Datum.array(
                listOf(
                    Datum.array(
                        listOf(
                            Datum.integer(1)
                        )
                    )
                )
            ) // [[1]]
        ),
        EquivValues(
            Datum.array(
                listOf(
                    Datum.array(
                        listOf(
                            Datum.integer(1), Datum.integer(1)
                        )
                    )
                )
            ) // [[1, 1]]
        ),
        EquivValues(
            Datum.array(
                listOf(
                    Datum.array(
                        listOf(
                            Datum.integer(1), Datum.integer(2)
                        )
                    )
                )
            ) // [[1, 2]]
        ),
        EquivValues(
            Datum.array(
                listOf(
                    Datum.array(
                        listOf(
                            Datum.integer(2), Datum.integer(1)
                        )
                    )
                )
            ) // [[2, 1]]
        ),
        EquivValues(
            Datum.array(
                listOf(
                    Datum.array(
                        listOf(
                            Datum.array(listOf(Datum.integer(1)))
                        )
                    )
                )
            ) // [[[1]]]
        ),
        EquivValues(
            emptyStruct(), // {}
            // TODO: emptyStruct(annotations = listOf("m", "n", "o")) // `m::n::o::{}`
        ),
        EquivValues(
            struct( // {'a': true, 'b': 1000, 'c': false}
                "a" to Datum.bool(true), "b" to Datum.integer(1000), "c" to Datum.bool(false)
            ),
            struct( // {'b': `1e3`, 'a': true, 'c': false}
                "b" to Datum.real(1000f), "a" to Datum.bool(true), "c" to Datum.bool(false)
            )
        ),
        EquivValues(
            struct( // {'b': 1000, 'c': false}
                "b" to Datum.integer(1000), "c" to Datum.bool(false)
            ),
            struct( // {'c': false, 'b': 1.00000000e3}
                "c" to Datum.bool(false), "b" to Datum.decimal(BigDecimal("1.00000000e3"))
            )
        ),
        EquivValues(
            struct( // {'c': false}
                "c" to Datum.bool(false)
            )
        ),
        EquivValues(
            struct( // {'d': 1, 'f': 2}
                "d" to Datum.integer(1), "f" to Datum.integer(2)
            )
        ),
        EquivValues(
            struct( // {'d': 2, 'e': 3, 'f': 4}
                "d" to Datum.integer(2),
                "e" to Datum.integer(3),
                "f" to Datum.integer(4)
            )
        ),
        EquivValues(
            struct( // {'d': 3, 'e': 2}
                "d" to Datum.integer(3),
                "e" to Datum.integer(2)
            )
        ),
        EquivValues(
            struct( // { 'm': [1, 1], 'n': [1, 1]}
                "m" to Datum.array(listOf(Datum.integer(1), Datum.integer(1))),
                "n" to Datum.array(listOf(Datum.integer(1), Datum.integer(1)))
            )
        ),
        EquivValues(
            struct( // { 'm': [1, 1], 'n': [1, 2]}
                "m" to Datum.array(listOf(Datum.integer(1), Datum.integer(1))),
                "n" to Datum.array(listOf(Datum.integer(1), Datum.integer(2)))
            )
        ),
        EquivValues(
            struct( // { 'm': [1, 1], 'n': [2, 2]}
                "m" to Datum.array(listOf(Datum.integer(1), Datum.integer(1))),
                "n" to Datum.array(listOf(Datum.integer(2), Datum.integer(2)))
            )
        ),
        EquivValues(
            struct( // { 'm': [1, 2], 'n': [2, 2]}
                "m" to Datum.array(listOf(Datum.integer(1), Datum.integer(2))),
                "n" to Datum.array(listOf(Datum.integer(2), Datum.integer(2)))
            )
        ),
        EquivValues(
            struct( // { 'm': [2, 2], 'n': [2, 2]}
                "m" to Datum.array(listOf(Datum.integer(2), Datum.integer(2))),
                "n" to Datum.array(listOf(Datum.integer(2), Datum.integer(2)))
            )
        ),
        EquivValues(
            struct( // { 'm': <<1, 1>>, 'n': []}
                "m" to Datum.bag(listOf(Datum.integer(1), Datum.integer(1))),
                "n" to emptyList
            )
        ),
        EquivValues(
            struct( // { 'm': <<1, 1>>, 'n': <<>>}
                "m" to Datum.bag(listOf(Datum.integer(1), Datum.integer(1))),
                "n" to emptyBag
            )
        ),
        EquivValues( // {'x': 1, 'y': 2}
            struct(
                "x" to Datum.integer(1),
                "y" to Datum.integer(2)
            )
        ),
        EquivValues( // {'x': 1, 'y': 2, 'z': 1}
            struct(
                "x" to Datum.integer(1),
                "y" to Datum.integer(2),
                "z" to Datum.integer(1)
            )
        ),
        EquivValues( // <<>>
            emptyBag
        ),
        EquivValues(
            // The ordered values are: true, true, 1
            // <<1, true, true>>
            Datum.bag(listOf(Datum.integer(1), Datum.bool(true), Datum.bool(true)))
        ),
        EquivValues(
            // The ordered values are: true, true, 1, 1, 1
            // <<true, 1, 1.0, `1e0`, true>>
            Datum.bag(listOf(Datum.bool(true), Datum.integer(1), Datum.decimal(BigDecimal("1.0")), Datum.real(1e0f), Datum.bool(true)))
        ),
        EquivValues( // <<1>>
            Datum.bag(listOf(Datum.integer(1)))
        ),
        EquivValues( // <<1, 1>>
            Datum.bag(listOf(Datum.integer(1), Datum.integer(1)))
        ),
        EquivValues( // << [] >>
            Datum.bag(listOf(emptyList))
        ),
        EquivValues( // << {}, [] >>
            Datum.bag(listOf(emptyStruct(), emptyList))
        ),
        EquivValues( // << {} >>
            Datum.bag(listOf(emptyStruct()))
        ),
        EquivValues( // << <<>> >>
            Datum.bag(listOf(emptyBag))
        ),
        EquivValues( // << <<>>, <<>> >>
            Datum.bag(listOf(emptyBag, emptyBag))
        )
    )

    companion object {
        fun struct(vararg pairs: Pair<String, Datum>): Datum {
            val fields = pairs.map { field ->
                Field.of(field.first, field.second)
            }
            return Datum.struct(fields)
        }
    }
}
