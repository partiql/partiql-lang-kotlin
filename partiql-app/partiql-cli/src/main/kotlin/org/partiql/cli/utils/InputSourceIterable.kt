package org.partiql.cli.utils

import com.amazon.ion.IonReader
import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonReaderBuilder
import org.partiql.lang.eval.ExprValue

/**
 * A closeable, iterable wrapper over an [InputSource] containing Ion values. With an [InputSourceIterable],
 * it is possible to create a new [Iterator] of [ExprValue]'s from an [InputSource] containing a sequence of Ion
 * values. By leveraging [InputSource]'s ability to re-open streams, the [InputSourceIterable] is able to create
 * iterators at the beginning of the [InputSource.stream].
 */
internal class InputSourceIterable(
    private val ion: IonSystem,
    private val input: InputSource
) : Iterable<ExprValue>, AutoCloseable {

    private val readers = mutableListOf<IonReader>()

    override fun iterator(): Iterator<ExprValue> {
        val reader = IonReaderBuilder.standard().build(input.stream())
        readers.add(reader)
        return ion.iterate(reader).asSequence().map { ExprValue.of(it) }.iterator()
    }

    override fun close() = readers.forEach { it.close() }
}
