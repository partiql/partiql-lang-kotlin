package org.partiql.spi

import com.amazon.ion.IonValue

interface RecordSource : AutoCloseable {

    // eh
    fun get(): Sequence<IonValue>

    override fun close() {
        // no-op
    }
}
