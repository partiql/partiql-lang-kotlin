package org.partiql.cli.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FormatTests {

    @ParameterizedTest
    @EnumSource
    internal fun singleFormatCanBeParsed(format: Format) {
        val simple = format.toString().lowercase()
        val parsed = Format.Converter.convert(simple)
        assertEquals(format, parsed.first)
    }
}
