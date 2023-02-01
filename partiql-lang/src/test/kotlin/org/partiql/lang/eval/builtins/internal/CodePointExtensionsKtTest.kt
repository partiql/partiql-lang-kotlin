package org.partiql.lang.eval.builtins.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CodePointExtensionsKtTest {

    @Test
    fun positionOf() {
        val a = intArrayOf(1, 2, 3, 4, 5, 6, 7)
        val b = intArrayOf(1, 2, 3)
        val c = intArrayOf(3, 4, 5)
        val d = intArrayOf()
        val e = intArrayOf(1, 1, 1)
        val f = intArrayOf(6, 7)
        // contains beginning
        assertEquals(1, a.positionOf(b))
        // contains middle
        assertEquals(3, a.positionOf(c))
        // contains end
        assertEquals(6, a.positionOf(f))
        // cannot contain
        assertEquals(0, b.positionOf(a))
        // does not contain
        assertEquals(0, a.positionOf(e))
        // always contains
        assertEquals(1, a.positionOf(d))
    }

    @Test
    fun overlay() {
        val a = "hello"
        val b = "XX"
        // OVERLAY('hello' PLACING '' FROM 1)
        assertEquals("hello", codepointOverlay(a, "", 1))
        // OVERLAY('hello' PLACING '' FROM 2 FOR 3);
        assertEquals("ho", codepointOverlay(a, "", 2, 3))
        // OVERLAY('hello' PLACING '' FROM 2 FOR 4);
        assertEquals("h", codepointOverlay(a, "", 2, 4))
        // OVERLAY('hello' PLACING 'XX' FROM 1)
        assertEquals("XXllo", codepointOverlay(a, b, 1))
        // OVERLAY('hello' PLACING 'XX' FROM 1 FOR 3)
        assertEquals("XXlo", codepointOverlay(a, b, 1, 3))
        // OVERLAY('hello' PLACING 'XX' FROM 1 FOR 1)
        assertEquals("XXello", codepointOverlay(a, b, 1, 1))
        // OVERLAY('hello' PLACING 'XX' FROM 1 FOR 100)
        assertEquals("XX", codepointOverlay(a, b, 1, 100))
        // OVERLAY('hello' PLACING 'XX' FROM 1 FOR 0)
        assertEquals("XXhello", codepointOverlay(a, b, 1, 0))
        // OVERLAY('hello' PLACING 'XX' FROM 7)
        assertEquals("helloXX", codepointOverlay(a, b, 7))
        // OVERLAY('hello' PLACING 'XX' FROM 100 FOR 100)
        assertEquals("helloXX", codepointOverlay(a, b, 100, 100))
        // OVERLAY('hello' PLACING 'XX' FROM 2 FOR 1)
        assertEquals("hXXllo", codepointOverlay(a, b, 2, 1))
        // OVERLAY('hello' PLACING 'XX' FROM 2 FOR 3)
        assertEquals("hXXo", codepointOverlay(a, b, 2, 3))
    }
}
