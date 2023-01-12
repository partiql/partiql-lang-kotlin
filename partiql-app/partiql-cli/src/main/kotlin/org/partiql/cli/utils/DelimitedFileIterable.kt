package org.partiql.cli.utils

import com.amazon.ion.IonSystem
import org.apache.commons.csv.CSVFormat
import org.partiql.cli.functions.ReadFile.Companion.conversionModeFor
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.io.DelimitedValues
import java.io.InputStreamReader

/**
 * A closeable, iterable wrapper over an [InputSource] containing Ion values. With an [DelimitedFileIterable],
 * it is possible to create a new [Iterator] of [ExprValue]'s from an [InputSource] containing a sequence of delimited
 * values. By leveraging [InputSource]'s ability to re-open streams, the [DelimitedFileIterable] is able to create
 * iterators at the beginning of the [InputSource.stream].
 */
internal class DelimitedFileIterable(
    private val ion: IonSystem,
    private val input: InputSource,
    private val format: CSVFormat,
    private val encoding: String,
    private val conversion: String
) : Iterable<ExprValue>, AutoCloseable {

    private val readers: MutableList<InputStreamReader> = mutableListOf()

    override fun iterator(): Iterator<ExprValue> {
        val reader = InputStreamReader(input.stream(), encoding)
        readers.add(reader)
        return DelimitedValues.exprValue(ion, reader, format, conversionModeFor(conversion)).iterator()
    }

    override fun close() {
        readers.forEach { it.close() }
    }
}
