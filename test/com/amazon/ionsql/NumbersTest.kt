/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import org.junit.Test
import java.math.BigDecimal

class NumbersTest : Base() {
    fun dec(text: String): BigDecimal = BigDecimal(text)
    fun dec(value: Double): BigDecimal = BigDecimal.valueOf(value)

    fun assertCoerce(num1: Number, num2: Number, expected1: Number, expected2: Number) {
        val (actual1, actual2) = coerceNumbers(num1, num2)
        assertEquals(expected1, actual1)
        assertEquals(expected2, actual2)
    }

    @Test fun coerceLongLong() = assertCoerce(1L, 2L, 1L, 2L)
    @Test fun coerceLongDouble() = assertCoerce(1L, 2.0, 1.0, 2.0)
    @Test fun coerceLongBigDecimal() = assertCoerce(1L, dec("10"), dec("1"), dec("10"))

    @Test fun coerceDoubleDouble() = assertCoerce(1.0, 2.0, 1.0, 2.0)
    @Test fun coerceDoubleLong() = assertCoerce(1.0, 2L, 1.0, 2.0)
    @Test fun coerceDoubleBigDecimal() = assertCoerce(1.0, dec("10"), dec(1.0), dec("10"))

    @Test fun coerceBigDecimalBigDecimal() =
        assertCoerce(dec("100"), dec("200"), dec("100"), dec("200"))
    @Test fun coerceBigDecimalLong() = assertCoerce(dec("1.1"), 2L, dec("1.1"), dec("2"))
    @Test fun coerceBigDecimalDouble() = assertCoerce(dec("1.1"), 2.0, dec("1.1"), dec(2.0))

    @Test fun unaryMinusLong() = assertEquals(-1L, Number::unaryMinus.call(1L))
    @Test fun unaryMinusDouble() = assertEquals(-1.0, Number::unaryMinus.call(1.0))
    @Test fun unaryMinusBigDecimal() =
        assertEquals(dec("-100.1"), Number::unaryMinus.call(dec("100.1")))

    @Test fun plusLong() = assertEquals(4L, Number::plus.call(1L, 3L))
    @Test fun plusDouble() = assertEquals(4.0, Number::plus.call(1L, 3.0))
    @Test fun plusBigDecimal() = assertEquals(dec("4.1"), Number::plus.call(1L, dec("3.1")))

    @Test fun minusLong() = assertEquals(-2L, Number::minus.call(1L, 3L))
    @Test fun minusDouble() = assertEquals(-2.0, Number::minus.call(1L, 3.0))
    @Test fun minusBigDecimal() = assertEquals(dec("-2.1"), Number::minus.call(1L, dec("3.1")))

    @Test fun timesLong() = assertEquals(3L, Number::times.call(1L, 3L))
    @Test fun timesDouble() = assertEquals(3.0, Number::times.call(1L, 3.0))
    @Test fun timesBigDecimal() = assertEquals(dec("3.1"), Number::times.call(1L, dec("3.1")))

    @Test fun divLong() = assertEquals(2L, Number::div.call(4L, 2L))
    @Test fun divDouble() = assertEquals(2.0, Number::div.call(4.0, 2L))
    @Test fun divBigDecimal() = assertEquals(dec("2"), Number::div.call(dec("4"), 2L))

    @Test fun modLong() = assertEquals(0L, Number::mod.call(4L, 2L))
    @Test fun modDouble() = assertEquals(0.0, Number::mod.call(4.0, 2L))
    @Test fun modBigDecimal() = assertEquals(dec("0"), Number::mod.call(dec("4"), 2L))

    @Test fun cmpLongLess() = assertTrue(Number::compareTo.call(1L, 2L) < 0)
    @Test fun cmpLongEquals() = assertEquals(0, Number::compareTo.call(1L, 1L))
    @Test fun cmpLongMore() = assertTrue(Number::compareTo.call(2L, 1L) > 0)

    @Test fun cmpDoubleLess() = assertTrue(Number::compareTo.call(1L, 2.0) < 0)
    @Test fun cmpDoubleEquals() = assertEquals(0, Number::compareTo.call(1.0, 1L))
    @Test fun cmpDoubleMore() = assertTrue(Number::compareTo.call(2L, 1.0) > 0)

    @Test fun cmpBigDecimalLess() = assertTrue(Number::compareTo.call(1L, dec("2")) < 0)
    @Test fun cmpBigDecimalEquals() = assertEquals(0, Number::compareTo.call(dec("1"), 1L))
    @Test fun cmpBigDecimalMore() = assertTrue(Number::compareTo.call(2L, dec("1")) > 0)
}