package org.partiql.cli.io

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.io.InputStream
import java.math.BigDecimal

/**
 * Reads CSV or TSV files into a [Datum] bag of structs.
 * The first row is treated as column headers.
 * Values are auto-typed: null, boolean, integer, decimal, or string.
 */
internal object DatumCsvReader {

    fun read(input: InputStream, delimiter: Char): Datum {
        val format = CSVFormat.DEFAULT
            .withHeader()
            .withSkipHeaderRecord()
            .withDelimiter(delimiter)
            .withIgnoreEmptyLines()
            .withTrim()
        val parser = CSVParser.parse(input, Charsets.UTF_8, format)
        return Datum.bag(
            Iterable {
                val iter = parser.iterator()
                object : Iterator<Datum> {
                    override fun hasNext(): Boolean {
                        val has = iter.hasNext()
                        if (!has) parser.close()
                        return has
                    }
                    override fun next(): Datum {
                        val record = iter.next()
                        val fields = record.toMap().map { (key, value) ->
                            Field.of(key, inferDatum(value))
                        }
                        return Datum.struct(fields)
                    }
                }
            }
        )
    }

    private fun inferDatum(value: String?): Datum {
        if (value.isNullOrEmpty()) return Datum.nullValue()
        // boolean
        if (value.equals("true", ignoreCase = true)) return Datum.bool(true)
        if (value.equals("false", ignoreCase = true)) return Datum.bool(false)
        // integer
        value.toLongOrNull()?.let { return Datum.bigint(it) }
        // decimal
        try {
            return Datum.decimal(BigDecimal(value))
        } catch (_: NumberFormatException) {
            // not a number
        }
        // string
        return Datum.string(value)
    }
}
