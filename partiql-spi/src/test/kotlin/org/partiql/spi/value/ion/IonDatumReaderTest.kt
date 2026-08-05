package org.partiql.spi.value.ion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

class IonDatumReaderTest {

    /**
     * Test we can parse all inputs with no errors.
     */
    @Test
    fun acceptance() {
        readAll("/io/ion/kitchen_lower.ion")
        // TODO re-enable after types with arguments are supported
        // readAll("/io/ion/kitchen_typed.ion")
    }

    @Test
    fun `reads null timestamp`() {
        assertTrue(read("null.timestamp").isNull)
    }

    @Test
    fun `reads date precision timestamps`() {
        listOf(
            "2007-01-01" to "2007-01-01",
            "2007-01-01T" to "2007-01-01",
            "2007-01T" to "2007-01-01",
            "2007T" to "2007-01-01",
            "2007-02-23" to "2007-02-23",
        ).forEach { (input, expected) ->
            val datum = read(input)

            assertEquals(PType.date(), datum.type, input)
            assertEquals(LocalDate.parse(expected), datum.localDate, input)
        }
    }

    @Test
    fun `reads timestamps with unknown offset`() {
        listOf(
            Triple("2007-02-23T12:14:33.079-00:00", "2007-02-23T12:14:33.079", 3),
            Triple("2007-01-01T00:00-00:00", "2007-01-01T00:00", 0),
            Triple("2007-02-23T00:00:00-00:00", "2007-02-23T00:00:00", 0),
        ).forEach { (input, expected, precision) ->
            val datum = read(input)

            assertEquals(PType.timestamp(precision), datum.type, input)
            assertEquals(LocalDateTime.parse(expected), datum.localDateTime, input)
        }
    }

    @Test
    fun `reads timestamps with known offset`() {
        listOf(
            "2007-02-23T12:14Z" to 0,
            "2007-02-23T12:14:33.079-08:00" to 3,
            "2007-02-23T20:14:33.079Z" to 3,
            "2007-02-23T20:14:33.079+00:00" to 3,
            "2007-02-23T00:00Z" to 0,
            "2007-02-23T00:00+00:00" to 0,
        ).forEach { (input, precision) ->
            val datum = read(input)

            assertEquals(PType.timestampz(precision), datum.type, input)
            assertEquals(OffsetDateTime.parse(input), datum.offsetDateTime, input)
        }
    }

    private fun read(input: String): Datum = DatumReader.ion(input.byteInputStream()).use { it.next()!! }

    private fun readAll(resource: String) {
        val input = this::class.java.getResourceAsStream(resource)!!
        val reader = DatumReader.ion(input)
        while (reader.next() != null) {
            // do nothing
        }
        reader.close()
    }
}
