package org.partiql.spi.value.ion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumReader
import java.time.OffsetDateTime
import java.time.ZoneOffset

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
    fun `reads timestamp with known offset`() {
        val datum = read("2007-02-23T12:14:33.079-08:30")

        assertEquals(PType.timestampz(9), datum.type)
        assertEquals(OffsetDateTime.parse("2007-02-23T12:14:33.079-08:30"), datum.offsetDateTime)
    }

    @Test
    fun `reads timestamp with unknown offset as UTC`() {
        val datum = read("2007-02-23T12:14:33.079-00:00")

        assertEquals(PType.timestampz(9), datum.type)
        assertEquals(
            OffsetDateTime.of(2007, 2, 23, 12, 14, 33, 79_000_000, ZoneOffset.UTC),
            datum.offsetDateTime,
        )
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
