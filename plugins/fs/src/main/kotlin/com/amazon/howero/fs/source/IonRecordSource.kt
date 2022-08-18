package com.amazon.howero.fs.source

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import org.partiql.spi.RecordSource
import java.io.InputStream

class IonRecordSource(
    private val ion: IonSystem,
    private val input: InputStream
) : RecordSource {

    override fun get(): Sequence<IonValue> {
        val reader = ion.newReader(input)
        var type = reader.next()
        return sequence {
            while (type != null) {
                yield(ion.newValue(reader))
                type = reader.next()
            }
        }
    }

    override fun close() {
        input.close()
    }

}
