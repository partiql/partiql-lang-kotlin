package org.partiql.runner.io

import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
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
    @ValueSource(
        strings = [
            "\$json::9223372036854775808", // an integer too large for BIGINT: write a decimal
            "\$json::1d3", // a decimal with an exponent
            "\$json::1d50000000",
            "\$json::1d-1000000000",
            "\$json::123456789012345678901234567890123456789.", // 39 digits
            "\$json::nan",
            "\$json::+inf",
            "\$json::-inf",
            "\$json::abc", // a symbol value
            "\$json::'abc'",
            "\$json::null.int",
            "\$json::null.struct",
            "\$json::{ t: 2024-01-01T }",
            "\$json::{{ aGVsbG8= }}",
            "\$json::{{ \"clob\" }}",
            "\$json::(1 2)",
            "\$json::\$json::1",
            "\$json::[\$json::1]",
            "\$json::{ a: \$json::1 }",
            "\$json::[\$missing::null]",
            "\$json::[\$bag::[1]]",
            "\$json::[\$map::string::integer::[]]",
            "\$json::[\$interval_ym::{ years: 1 }]",
            "\$json::[\$interval_dt::{ days: 1 }]",
            "\$json::[\$graph::{}]",
            "\$json::[a::1]", // a user annotation
            "\$json::\$date::null",
            "\$json::\$date::\"2024-01-01\"",
            "\$json::\$date::{ year: \$json::2024, month: 1, day: 1 }",
            "\$date::\$json::{ year: 2024, month: 1, day: 1 }",
        ]
    )
    fun contentTheDocForbidsFailsToLoad(ion: String) {
        val e = assertThrows<IllegalArgumentException> { read(ion) }
        assertTrue(e.message!!.startsWith("Invalid \$json value: "), "$ion: ${e.message}")
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
