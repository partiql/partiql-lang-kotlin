package org.partiql.runner.io

import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.partiql.runner.util.resultMatches
import org.partiql.runner.util.toIonElement
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.JsonKind
import org.partiql.value.io.DatumIonReaderBuilder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Reading the `$json` annotation, as docs/partiql-tests-schema-proposal.md in partiql-tests defines it.
 */
@Suppress("DEPRECATION")
class JsonAnnotationTest {

    private val comparator = Datum.comparator(true, true)

    private fun read(ion: String): Datum = DatumIonReaderBuilder.standard().build(loadSingleElement(ion)).read()

    @Test
    fun readsAnObjectWithRepeatedKeysInOrder() {
        val v = read("\$json::{ a: 1, b: null, a: 2 }")
        assertEquals(PType.JSON, v.type.code())
        assertEquals(JsonKind.OBJECT, v.jsonKind)
        val members = v.fields.asSequence().toList()
        assertEquals(listOf("a", "b", "a"), members.map { it.name })
        assertEquals(JsonKind.NULL, members[1].value.jsonKind)
    }

    @Test
    fun integersAreIntUpTo32BitsThenBigint() {
        val v = read("\$json::[2147483647, -2147483648, 2147483648, -2147483649, 9223372036854775807, -9223372036854775808]")
        assertEquals(
            listOf(PType.INTEGER, PType.INTEGER, PType.BIGINT, PType.BIGINT, PType.BIGINT, PType.BIGINT),
            v.map { it.jsonScalar.type.code() },
        )
    }

    @Test
    fun otherNumbersGetTheTypeOfTheSameLiteral() {
        val v = read("\$json::[9223372036854775808., 10.5, 0.000, 1e3]")
        assertEquals(listOf(PType.DECIMAL, PType.DECIMAL, PType.DECIMAL, PType.DOUBLE), v.map { it.jsonScalar.type.code() })
        assertEquals(3, v.toList()[2].jsonScalar.bigDecimal.scale())
    }

    @Test
    fun nullIsSqlJsonNull() {
        val v = read("\$json::null")
        assertEquals(JsonKind.NULL, v.jsonKind)
        assertFalse(v.isNull)
    }

    @Test
    fun datetimesAtTheTopAndInside() {
        assertEquals(PType.DATE, read("\$json::\$date::{ year: 2024, month: 1, day: 1 }").jsonScalar.type.code())
        val d = read("\$json::{ d: \$date::{ year: 2024, month: 1, day: 1 } }").fields.next().value
        assertEquals(PType.DATE, d.jsonScalar.type.code())
    }

    @Test
    fun aNullOffsetHasNoTimeZone() {
        val time = "\$json::\$time::{ hour: 2, minute: 30, second: 59., offset: %s }"
        val ts = "\$json::\$timestamp::{ year: 2024, month: 1, day: 1, hour: 2, minute: 30, second: 59., offset: %s }"
        assertEquals(PType.TIME, read(time.format("null")).jsonScalar.type.code())
        assertEquals(PType.TIMEZ, read(time.format("0")).jsonScalar.type.code())
        assertEquals(PType.TIMESTAMP, read(ts.format("null")).jsonScalar.type.code())
        assertEquals(PType.TIMESTAMPZ, read(ts.format("0")).jsonScalar.type.code())
        assertFalse(resultMatches(read(time.format("0")), read(time.format("null")), comparator))
        assertFalse(resultMatches(read(ts.format("0")), read(ts.format("null")), comparator))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '`',
        textBlock = """
        ~json::9223372036854775808                                    | too large for BIGINT
        ~json::1d3                                                    | negative scale
        ~json::1d50000000                                             | negative scale
        ~json::1d-1000000000                                          | at most 38 digits
        ~json::123456789012345678901234567890123456789.               | at most 38 digits
        ~json::0.000000000000000000000000000000000000001              | at most 38 digits
        ~json::nan                                                    | NaN is not allowed
        ~json::+inf                                                   | Infinity is not allowed
        ~json::-inf                                                   | -Infinity is not allowed
        ~json::abc                                                    | an Ion symbol
        ~json::'abc'                                                  | an Ion symbol
        ~json::null.int                                               | only an untyped null
        ~json::null.struct                                            | only an untyped null
        ~json::{ t: 2024-01-01T }                                     | an Ion timestamp
        ~json::{{ aGVsbG8= }}                                         | an Ion blob
        ~json::{{ "clob" }}                                           | an Ion clob
        ~json::(1 2)                                                  | an Ion sexp
        ~json::~json::1                                               | a nested ~json
        ~json::[~json::1]                                             | a nested ~json
        ~json::{ a: ~json::1 }                                        | a nested ~json
        ~json::[~missing::null]                                       | [~missing] are not allowed
        ~json::[~bag::[1]]                                            | [~bag] are not allowed
        ~json::[~map::string::integer::[]]                            | [~map, string, integer] are not allowed
        ~json::[~interval_ym::{ years: 1 }]                           | [~interval_ym] are not allowed
        ~json::[~interval_dt::{ days: 1 }]                            | [~interval_dt] are not allowed
        ~json::[~graph::{}]                                           | [~graph] are not allowed
        ~json::[a::1]                                                 | [a] are not allowed
        ~date::~json::{ year: 2024, month: 1, day: 1 }                | must be the outermost annotation
        ~json::~date::null                                            | ~date must annotate a struct, got null
        ~json::~date::"2024-01-01"                                    | ~date must annotate a struct, got string
        ~json::~date::{ year: a::2024, month: 1, day: 1 }             | can't have annotations
        ~json::~date::{ year: ~json::2024, month: 1, day: 1 }         | can't have annotations
        ~json::~date::{ year: 2024, month: 1 }                        | ~date has no field day
        ~json::~date::{ year: 2024, month: 1, day: 1, hour: 1 }       | ~date has no field hour
        ~json::~date::{ year: 2024, month: 1, day: 1, day: 2 }        | repeats the field day
        ~json::~date::{ year: 2024, month: 13, day: 1 }               | ~date is not a valid date
        ~json::~date::{ year: 2024, month: 1.0, day: 1 }              | must be an Ion int
        ~json::~date::{ year: 99999999999, month: 1, day: 1 }         | out of range
        ~json::~time::{ hour: 2, minute: 30, second: 59, offset: 0 }  | must be an Ion decimal
        ~json::~time::{ hour: 2, minute: 30, second: 60., offset: 0 } | less than 60
        ~json::~time::{ hour: 2, minute: 30, second: 1.0000000001, offset: 0 } | at most 9 digits after the point
        ~json::~time::{ hour: 2, minute: 30, second: 1., offset: "0" }  | must be an Ion int or null
        ~json::~time::{ hour: 2, minute: 30, second: 1. }             | ~time has no field offset
        ~json::~time::{ hour: 25, minute: 30, second: 1., offset: 0 } | ~time is not a valid time
        ~json::~time::{ hour: 2, minute: 30, second: 1., offset: 5000 } | ~time is not a valid time"""
    )
    fun contentTheDocForbidsFailsToLoad(ionWithTildes: String, reasonWithTildes: String) {
        // The table writes $ as ~, so Kotlin doesn't read $json as a string template.
        val ion = ionWithTildes.replace('~', '$')
        val reason = reasonWithTildes.replace('~', '$')
        val e = assertThrows<IllegalArgumentException> { read(ion) }
        assertTrue(e.message!!.startsWith("Invalid \$json value: "), "$ion: ${e.message}")
        assertTrue(e.message!!.contains(reason), "$ion was rejected, but for another reason: ${e.message}")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "\$json::12345678901234567890123456789012345678.", // 38 digits
            "\$json::0.00000000000000000000000000000000000001", // scale 38
            "\$json::1.5d0",
            "\$json::1.00d2",
            "\$json::1.5d1",
        ]
    )
    fun decimalsAtTheLimitsLoad(ion: String) {
        assertEquals(PType.DECIMAL, read(ion).jsonScalar.type.code())
    }

    @Test
    fun errorMessagesStayShort() {
        val e = assertThrows<IllegalArgumentException> { read("\$json::" + "9".repeat(100_000)) }
        assertTrue(e.message!!.length < 200, e.message)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "\$json::{ a: [1, null, \"x\", true], b: { c: 10.50 }, d: 1e3 }",
            "\$json::\$date::{ year: 2024, month: 1, day: 1 }",
            "\$json::[\$time::{ hour: 2, minute: 30, second: 59.5, offset: null }, \$time::{ hour: 2, minute: 30, second: 59., offset: -480 }]",
            "\$json::{ t: \$timestamp::{ year: 2024, month: 1, day: 1, hour: 2, minute: 30, second: 0., offset: null } }",
            "\$json::\$timestamp::{ year: 2024, month: 1, day: 1, hour: 2, minute: 30, second: 0.25, offset: 330 }",
            "\$json::null",
        ]
    )
    fun aReportedValueReadsBackAsTheSameValue(ion: String) {
        val value = read(ion)
        val written = value.toIonElement()
        assertEquals("\$json", written.annotations.first())
        val reread = DatumIonReaderBuilder.standard().build(written).read()
        assertTrue(resultMatches(reread, value, comparator), "$ion was written as $written")
    }
}
