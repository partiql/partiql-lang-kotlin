/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF DYNAMIC KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.partiql.eval.internal.routines.internal

import com.amazon.ion.Decimal
import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Aggregation
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValueType
import org.partiql.value.util.coerceNumbers
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

internal abstract class Accumulator : Accumulator {

    /** Accumulates the next value into this [Accumulator]. */
    override fun next(args: Array<Datum>) {
        val value = args[0]
        if (value.isAbsent()) return
        nextValue(value)
    }

    abstract fun nextValue(value: Datum)
}

internal fun comparisonAccumulator(comparator: Comparator<Datum>): (Datum?, Datum) -> Datum =
    { left, right ->
        when {
            left == null || comparator.compare(left, right) > 0 -> right
            else -> left
        }
    }

// internal fun checkIsNumberType(funcName: String, value: Datum) {
//     if (!value.type.isNumber()) {
//         throw TypeCheckException("Expected NUMBER but received ${value.type}.")
//     }
// }

internal operator fun Number.plus(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.checkOverflowPlus(second as Long)
        is Double -> first + second as Double
        is BigDecimal -> first.add(second as BigDecimal, MATH_CONTEXT)
        is BigInteger -> first.add(second as BigInteger)
        else -> throw IllegalStateException()
    }
}

internal operator fun Number.div(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.checkOverflowDivision(second as Long)
        is Double -> first / second as Double
        is BigDecimal -> first.divide(second as BigDecimal, MATH_CONTEXT)
        else -> throw IllegalStateException()
    }
}

private fun Long.checkOverflowDivision(other: Long): Number {
    // division can only underflow Long.MIN_VALUE / -1
    // because abs(Long.MIN_VALUE) == abs(Long.MAX_VALUE) + 1
    if (this == Long.MIN_VALUE && other == -1L) {
        error("Division overflow or underflow.")
    }

    return this / other
}

private fun Long.checkOverflowPlus(other: Long): Number {
    // uses to XOR to check if
    // this and other are >= 0 then if result < 0 means overflow
    // this and other are < 0 then if result > 0 means underflow
    // if this and other have different signs then no overflow can happen

    val result: Long = this + other
    val overflows = ((this xor other) >= 0) and ((this xor result) < 0)
    return when (overflows) {
        false -> result
        else -> error("Int overflow or underflow")
    }
}

internal fun checkIsBooleanType(funcName: String, value: Datum) {
    if (value.type != PartiQLValueType.BOOL) {
        throw TypeCheckException("Expected ${PartiQLValueType.BOOL} but received ${value.type}.")
    }
}

internal fun Datum.isAbsent(): Boolean = this.type == PartiQLValueType.MISSING || this.isNull

internal fun Datum.numberValue(): Number = when (this.type) {
    PartiQLValueType.TINYINT -> byte
    PartiQLValueType.SMALLINT -> short
    PartiQLValueType.INT -> int
    PartiQLValueType.BIGINT -> long
    PartiQLValueType.INT -> bigInteger
    PartiQLValueType.DECIMAL -> bigDecimal
    PartiQLValueType.DECIMAL_ARBITRARY -> bigDecimal
    PartiQLValueType.FLOAT32 -> float
    PartiQLValueType.FLOAT64 -> double
    else -> error("Cannot convert Datum ($this) to number.")
}

internal fun Datum.isNumber(): Boolean = when (this.type) {
    PartiQLValueType.INT,
    PartiQLValueType.TINYINT,
    PartiQLValueType.SMALLINT,
    PartiQLValueType.INT,
    PartiQLValueType.BIGINT,
    PartiQLValueType.DECIMAL,
    PartiQLValueType.DECIMAL_ARBITRARY,
    PartiQLValueType.FLOAT32,
    PartiQLValueType.FLOAT64,
    -> true
    else -> false
}

/**
 * This is specifically for SUM/AVG
 */

// internal fun nullToTargetType(type: PartiQLValueType): Datum = when (type) {
//     PartiQLValueType.ANY -> nullValue()
//     PartiQLValueType.FLOAT32 -> Datum.floatValue(null)
//     PartiQLValueType.FLOAT64 -> float64Value(null)
//     PartiQLValueType.TINYINT -> int8Value(null)
//     PartiQLValueType.SMALLINT -> int16Value(null)
//     PartiQLValueType.INT -> int32Value(null)
//     PartiQLValueType.BIGINT -> int64Value(null)
//     PartiQLValueType.INT -> intValue(null)
//     PartiQLValueType.DECIMAL_ARBITRARY, PartiQLValueType.DECIMAL -> decimalValue(null)
//     else -> TODO("Unsupported target type $type")
// }

/**
 * This is specifically for SUM/AVG
 */

// internal fun Number.toTargetType(type: PartiQLValueType): Datum = when (type) {
//     PartiQLValueType.DYNAMIC -> this.partiqlValue()
//     PartiQLValueType.FLOAT32 -> float32Value(this.toFloat())
//     PartiQLValueType.FLOAT64 -> float64Value(this.toDouble())
//     PartiQLValueType.DECIMAL, PartiQLValueType.DECIMAL_ARBITRARY -> {
//         when (this) {
//             is BigDecimal -> decimalValue(this)
//             is BigInteger -> decimalValue(this.toBigDecimal())
//             else -> decimalValue(BigDecimal.valueOf(this.toDouble()))
//         }
//     }
//     PartiQLValueType.TINYINT -> int8Value(this.toByte())
//     PartiQLValueType.SMALLINT -> int16Value(this.toShort())
//     PartiQLValueType.INT -> int32Value(this.toInt())
//     PartiQLValueType.BIGINT -> int64Value(this.toLong())
//     PartiQLValueType.INT -> when (this) {
//         is BigInteger -> intValue(this)
//         is BigDecimal -> intValue(this.toBigInteger())
//         else -> intValue(BigInteger.valueOf(this.toLong()))
//     }
//     else -> TODO("Unsupported target type $type")
// }

// internal fun Number.partiqlValue(): Datum = when (this) {
//     is Int -> int32Value(this)
//     is Long -> int64Value(this)
//     is Double -> float64Value(this)
//     is BigDecimal -> decimalValue(this)
//     is BigInteger -> intValue(this)
//     else -> TODO("Could not convert $this to PartiQL Value")
// }

private val MATH_CONTEXT = MathContext(38, RoundingMode.HALF_EVEN)

/**
 * Factory function to create a [BigDecimal] using correct precision, use it in favor of native BigDecimal constructors
 * and factory methods
 */
internal fun bigDecimalOf(num: Number, mc: MathContext = MATH_CONTEXT): BigDecimal = when (num) {
    is Decimal -> num
    is Int -> BigDecimal(num, mc)
    is Long -> BigDecimal(num, mc)
    is Double -> BigDecimal(num, mc)
    is BigDecimal -> num
    Decimal.NEGATIVE_ZERO -> num as Decimal
    else -> throw IllegalArgumentException("Unsupported number type: $num, ${num.javaClass}")
}
