package org.partiql.parser.internal.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.parser.internal.util.NumberUtils.negate
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.decimalValue
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import java.math.BigDecimal
import java.math.BigInteger

@OptIn(PartiQLValueExperimental::class)
class NumberUtilsTest {

    @Test
    fun negate_normal() {
        assertEquals(int8Value(-1), int8Value(1).negate())
        assertEquals(int16Value(-1), int16Value(1).negate())
        assertEquals(int32Value(-1), int32Value(1).negate())
        assertEquals(int64Value(-1), int64Value(1).negate())
        assertEquals(intValue(BigInteger.valueOf(-1L)), intValue(BigInteger.valueOf(1L)).negate())
        assertEquals(decimalValue(BigDecimal.valueOf(-1L)), decimalValue(BigDecimal.valueOf(1L)).negate())
    }

    @Test
    fun negate_overflow() {
        assertEquals(int16Value((Byte.MAX_VALUE.toShort() + 1).toShort()), int8Value(Byte.MIN_VALUE).negate())
        assertEquals(int32Value((Short.MAX_VALUE.toInt() + 1)), int16Value(Short.MIN_VALUE).negate())
        assertEquals(int64Value((Int.MAX_VALUE.toLong() + 1)), int32Value(Int.MIN_VALUE).negate())
        assertEquals(intValue(BigInteger.valueOf(Long.MAX_VALUE) + BigInteger.ONE), int64Value(Long.MIN_VALUE).negate())
    }
}
