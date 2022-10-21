package org.partiql.ionschema.parser

import kotlin.test.fail

fun <T> assertDoesNotThrow(label: String, block: () -> T) =
    try {
        block()
    } catch (ex: Throwable) {
        ex.printStackTrace()
        fail("Expected no exception but '$label' threw an exception, see console output for stack trace")
    }

inline fun <reified T : Exception> assertThrows(label: String, block: () -> Unit): T {
    try {
        block()
        fail("$label: expected an instance of ${T::class.java} to be thrown but nothing was thrown")
    } catch (ex: Throwable) {
        if (!T::class.java.isAssignableFrom(ex.javaClass)) {
            ex.printStackTrace()
            fail("$label: expected an instance of ${T::class.java} to be thrown but instead ${ex.javaClass} was thrown")
        }
        return ex as T
    }
}
