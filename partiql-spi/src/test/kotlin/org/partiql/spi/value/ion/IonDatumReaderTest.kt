package org.partiql.spi.value.ion

import org.junit.jupiter.api.Test
import org.partiql.spi.value.DatumReader

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

    private fun readAll(resource: String) {
        val input = this::class.java.getResourceAsStream(resource)!!
        val reader = DatumReader.ion(input)
        while (reader.next() != null) {
            // do nothing
        }
        reader.close()
    }
}
