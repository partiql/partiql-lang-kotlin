package org.partiql.runner.io

import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
 * The `$json` annotation, as docs/partiql-tests-schema-proposal.md in partiql-tests defines it.
 */
@Suppress("DEPRECATION")
class JsonAnnotationTest {

    private val comparator = Datum.comparator(true, true)

    private fun read(ion: String): Datum = DatumIonReaderBuilder.standard().build(loadSingleElement(ion)).read()

    private fun matches(actual: String, expect: String) = resultMatches(read(actual), read(expect), comparator)

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
    fun numbersGetTheTypeOfTheSameLiteral() {
        val types = read("\$json::[7, 3000000000, 12345678901234567890123., 10.5, 1e3]").map { it.jsonScalar.type.code() }
        assertEquals(listOf(PType.INTEGER, PType.BIGINT, PType.DECIMAL, PType.DECIMAL, PType.DOUBLE), types)
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
    fun contentTheDocForbidsFailsToLoad() {
        listOf(
            "\$json::9223372036854775808", // too large for BIGINT: write a decimal
            "\$json::nan",
            "\$json::+inf",
            "\$json::abc", // a symbol value
            "\$json::null.int",
            "\$json::{ t: 2024-01-01T }",
            "\$json::{{ aGVsbG8= }}",
            "\$json::(1 2)",
            "\$json::[\$json::1]",
            "\$json::[\$bag::[1]]",
            "\$date::\$json::{ year: 2024, month: 1, day: 1 }",
        ).forEach { ion -> assertThrows<IllegalArgumentException>(ion) { read(ion) } }
    }

    @Test
    fun objectsMatchInAnyOrderAndCountRepeats() {
        assertTrue(matches("\$json::{ a: 1, b: 2 }", "\$json::{ b: 2, a: 1 }"))
        assertFalse(matches("\$json::{ a: 1, a: 1 }", "\$json::{ a: 1 }"))
        assertFalse(matches("\$json::[1, 2]", "\$json::[2, 1]"))
    }

    @Test
    fun numbersMatchByTypeValueAndScale() {
        assertTrue(matches("\$json::7", "\$json::7"))
        assertFalse(matches("\$json::7", "\$json::7.0"))
        assertFalse(matches("\$json::10.5", "\$json::10.50"))
    }

    @Test
    fun nullsMatchOnlyTheirOwnKind() {
        assertFalse(matches("\$json::null", "null"))
        assertFalse(matches("null", "\$json::null"))
        assertTrue(resultMatches(Datum.nullValue(PType.json()), read("null"), comparator))
    }

    @Test
    fun aJsonValueNeverMatchesAnotherType() {
        assertFalse(matches("{ a: 1 }", "\$json::{ a: 1 }"))
        assertFalse(matches("\$json::\"x\"", "\"x\""))
    }

    @Test
    fun containersOfJsonMatchDeeply() {
        assertTrue(matches("\$bag::[{ a: \$json::[1] }, 2]", "\$bag::[2, { a: \$json::[1] }]"))
        assertFalse(matches("\$bag::[{ a: \$json::[1] }]", "\$bag::[{ a: \$json::[2] }]"))
    }

    @Test
    fun writesJsonBackAsIon() {
        val ion = read("\$json::{ a: [1, null], d: \$date::{ year: 2024, month: 1, day: 1 } }").toIonElement()
        assertEquals(listOf("\$json"), ion.annotations)
    }
}
