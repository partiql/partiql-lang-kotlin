package org.partiql.cli.io

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * The CLI reads values of type JSON from Ion annotated with $json (partiql-tests' docs/partiql-tests-schema-proposal.md),
 * and prints them as PartiQL that builds them again.
 */
@Suppress("DEPRECATION")
class JsonIoTest {

    private fun read(ion: String): Datum = DatumIonReaderBuilder.standard().build(ion.byteInputStream()).read()

    private fun print(datum: Datum): String = StringBuilder().also { DatumWriterTextPretty(it).write(datum) }.toString()

    @Test
    fun readsAnObjectAsJson() {
        val d = read("""${'$'}json::{ id: 7, items: [ { sku: "A1", price: 10.5 } ], note: null }""")
        assertEquals(PType.JSON, d.type.code())
        assertEquals("""JSON('{"id": 7, "items": [{"sku": "A1", "price": 10.5}], "note": null}')""", print(d))
    }

    @Test
    fun aJsonValueInsideATupleIsReadToo() {
        val d = read("""{ t: ${'$'}json::[1, "x"] }""")
        assertEquals(PType.JSON, d.get("t").type.code())
    }

    @Test
    fun jsonNullIsAnSqlJsonNullNotSqlNull() {
        val d = read("${'$'}json::null")
        assertEquals(false, d.isNull)
        assertEquals("JSON('null')", print(d))
    }

    @Test
    fun aRepeatedKeyKeepsBothMembers() {
        assertEquals("""JSON('{"a": 1, "a": 2}')""", print(read("${'$'}json::{ a: 1, a: 2 }")))
    }

    @Test
    fun aDoubleKeepsAnExponentSoItReadsBackAsDouble() {
        assertEquals("JSON('[1.5e0, 1.0E10, 2.5]')", print(read("${'$'}json::[1.5e0, 1e10, 2.5]")))
    }

    @Test
    fun aLongExactNumberKeepsEveryDigit() {
        assertEquals(
            "JSON('1234567890123456789012345678901234567890.5')",
            print(read("${'$'}json::1234567890123456789012345678901234567890.5")),
        )
    }

    @Test
    fun stringsAreEscapedForJsonAndThenForPartiQL() {
        val d = Datum.jsonScalar(Datum.string("it's \"q\"\n\u0001"))
        assertEquals("""JSON('"it''s \"q\"\n\u0001"')""", print(d))
    }

    @Test
    fun aDatetimeIsWrittenWithJsonScalar() {
        assertEquals("JSON_SCALAR(DATE '2024-01-01')", print(read("${'$'}json::${'$'}date::{ year: 2024, month: 1, day: 1 }")))
    }

    @Test
    fun anItemHoldingADatetimeIsWrittenWithConstructors() {
        val d = Datum.jsonObject(
            listOf(
                Field.of("d", Datum.jsonScalar(Datum.date(LocalDate.of(2024, 1, 1)))),
                Field.of("n", Datum.jsonNull()),
                Field.of("xs", Datum.jsonArray(listOf(Datum.jsonScalar(Datum.integer(1)), Datum.jsonNull()))),
            )
        )
        assertEquals(
            "JSON_OBJECT('d' : JSON_SCALAR(DATE '2024-01-01'), 'n' : JSON('null'), 'xs' : JSON('[1, null]') RETURNING JSON)",
            print(d),
        )
    }

    @Test
    fun anArrayHoldingADatetimeKeepsItsNullElements() {
        val d = Datum.jsonArray(listOf(Datum.jsonScalar(Datum.date(LocalDate.of(2024, 1, 1))), Datum.jsonNull()))
        assertEquals("JSON_ARRAY(JSON_SCALAR(DATE '2024-01-01'), JSON('null') NULL ON NULL RETURNING JSON)", print(d))
    }

    @Test
    fun sqlNullOfTypeJsonPrintsNull() {
        assertEquals("null", print(Datum.nullValue(PType.json())))
    }

    @Test
    fun aJsonValueInsideABagIsIndented() {
        val d = Datum.bagVararg(Datum.jsonScalar(Datum.decimal(BigDecimal("10.5"), 3, 1)))
        assertEquals("<<\n  JSON('10.5')\n>>", print(d))
    }

    @Test
    fun invalidJsonIsRejected() {
        for (ion in listOf("${'$'}json::{ a: ${'$'}json::1 }", "${'$'}json::2024-01-01T", "${'$'}json::nan", "${'$'}date::${'$'}json::1")) {
            val e = assertThrows<Exception> { read(ion) }
            assert(generateSequence<Throwable>(e) { it.cause }.any { "Invalid \$json value" in (it.message ?: "") }) { "$ion: $e" }
        }
    }
}
