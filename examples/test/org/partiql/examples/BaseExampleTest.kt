package org.partiql.examples

import org.junit.Assert
import org.junit.Test
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

        Assert.assertEquals(expected, outBuffer.toString("UTF-8"))
    }
}