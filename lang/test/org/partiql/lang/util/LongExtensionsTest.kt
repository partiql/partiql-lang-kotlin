package org.partiql.lang.util

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
import java.lang.IllegalStateException

class LongExtensionsTest {

    @Test
    fun `int too high`() {
        assertThrows(IllegalStateException::class.java) { (Int.MAX_VALUE.toLong() + 1L).toIntExact() }
    }
    @Test
    fun `int too low`() {
        assertThrows(IllegalStateException::class.java) { (Int.MIN_VALUE.toLong() - 1L).toIntExact() }
    }

    @Test
    fun `can convert 0L`() {
        val value = assertDoesNotThrow { 0L.toIntExact() }
        assertEquals(0, value)
    }

    @Test
    fun `can convert 1L`() {
        val value = assertDoesNotThrow { 0L.toIntExact() }
        assertEquals(0, value)
    }

    @Test
    fun `can convert -1L`() {
        val value = assertDoesNotThrow { 0L.toIntExact() }
        assertEquals(0, value)
    }
}