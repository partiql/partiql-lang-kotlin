package org.partiql.examples

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.examples.util.Example
import java.io.ByteArrayOutputStream
import java.io.PrintStream

abstract class BaseExampleTest {
    abstract fun example(out: PrintStream): Example
    abstract val expected: String

    @Test
    fun test() {
        val outBuffer = ByteArrayOutputStream()

        example(PrintStream(outBuffer)).run()

        assertEquals(expected, outBuffer.toString("UTF-8"))
    }
}
