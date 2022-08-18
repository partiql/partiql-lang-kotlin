package com.amazon.howero.fs.source

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import org.partiql.spi.RecordSource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class TextRecordSource(
    private val ion: IonSystem,
    private val input: InputStream
) : RecordSource {

    override fun get(): Sequence<IonValue> {
        var n = 0
        val lines = BufferedReader(InputStreamReader(input))
        return sequence {
            while (lines.ready()) {
                val s = ion.newEmptyStruct()
                s.add("line", ion.newString(lines.readLine()))
                s.add("n", ion.newInt(n++))
                yield(s)
            }
        }
    }

    override fun close() {
        input.close()
    }
}