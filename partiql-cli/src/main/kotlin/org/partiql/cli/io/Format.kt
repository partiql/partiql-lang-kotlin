package org.partiql.cli.io

import picocli.CommandLine

internal enum class Format {
    PARTIQL,
    ION,
    ION_BINARY,
    JSON,
    CSV,
    TSV;

    object Converter : CommandLine.ITypeConverter<Pair<Format, Format>> {

        private const val ENUM = "[a-z][a-z0-9_]*"
        private const val PATTERN = "($ENUM)(:$ENUM)?"

        override fun convert(value: String?): Pair<Format, Format> {
            if (value == null) {
                return PARTIQL to PARTIQL
            }
            if (value.matches(Regex(PATTERN))) {
                error("Format argument does not match $PATTERN")
            }
            val str = value.trim().uppercase()
            val i: Format
            val o: Format
            if (str.contains(":")) {
                // two parts
                val parts = str.split(":")
                i = Format.valueOf(parts[0])
                o = Format.valueOf(parts[1])
            } else {
                //
                i = Format.valueOf(str)
                o = i
            }
            return i to o
        }
    }
}
