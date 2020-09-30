/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.util

import com.amazon.ion.*
import org.partiql.lang.eval.*
import java.math.*

private val MATH_CONTEXT = MathContext(38, RoundingMode.HALF_EVEN) // TODO should this be configurable?

/**
 * Factory function to create a [BigDecimal] using correct precision, use it in favor of native BigDecimal constructors
 * and factory methods
 */
internal fun bigDecimalOf(num: Number, mc: MathContext = MATH_CONTEXT): BigDecimal = when (num) {
    is Decimal            -> num
    is Int                -> BigDecimal(num, mc)
    is Long               -> BigDecimal(num, mc)
    is Double             -> BigDecimal(num, mc)
    is BigDecimal         -> num
    else                  -> throw IllegalArgumentException("Unsupported number type: $num, ${num.javaClass}")
}

internal fun bigDecimalOf(text: String, mc: MathContext = MATH_CONTEXT): BigDecimal = BigDecimal(text.trim(), mc)

private val CONVERSION_MAP = mapOf<Set<Class<*>>, Class<out Number>>(
    setOf(Long::class.javaObjectType, Long::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Long::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Long::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,

    setOf(Double::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,

    setOf(BigDecimal::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType)


private val CONVERTERS = mapOf<Class<*>, (Number) -> Number>(
    Long::class.javaObjectType      to Number::toLong,
    Double::class.javaObjectType    to Number::toDouble,
    BigDecimal::class.java to { num ->
        when (num) {
            is Long -> bigDecimalOf(num)
            is Double -> bigDecimalOf(num)
            is BigDecimal -> bigDecimalOf(num)
            else -> throw IllegalArgumentException(
                "Unsupported number for decimal conversion: $num"
            )
        }
    }
)

internal fun Number.isZero() = when(this) {
    // using compareTo instead of equals for BigDecimal because equality also checks same scale

    is Long -> this == 0L
    is Double -> this == 0.0 || this == -0.0 
    is BigDecimal -> BigDecimal.ZERO.compareTo(this) == 0
    else -> throw IllegalStateException()
}

/** Provides a narrowing or widening operator on supported numbers. */
fun Number.coerce(type: Class<out Number>): Number {
    val conv = CONVERTERS[type] ?: throw IllegalArgumentException("No converter for $type")
    return conv(this)
}

/**
 * Implements a very simple number tower to convert two numbers to their arithmetic
 * compatible type.
 *
 * This is only supported on limited types needed by the expression system.
 */
fun coerceNumbers(first: Number, second: Number): Pair<Number, Number> {
    fun typeFor(n: Number): Class<*> = if(n is Decimal) {
        BigDecimal::class.javaObjectType
    } else {
        n.javaClass
    }

    val type = CONVERSION_MAP[setOf(typeFor(first), typeFor(second))] ?:
               throw IllegalArgumentException("No coercion support for ${first to second}")

    return Pair(first.coerce(type), second.coerce(type))
}

fun Number.ionValue(ion: IonSystem): IonValue = when (this) {
    is Long -> ion.newInt(this)
    is BigInteger -> ion.newInt(this)
    is Double -> ion.newFloat(this)
    is BigDecimal -> ion.newDecimal(this)
    else -> throw IllegalArgumentException("Cannot convert to IonValue: $this")
}

operator fun Number.unaryMinus(): Number {
    return when (this) {
        is Long -> -this
        is BigInteger -> this.negate()
        is Double -> -this
        is BigDecimal -> if(this.isZero()) {
                Decimal.negativeZero(this.scale())
            } else {
                this.negate()
            }
        else -> throw IllegalStateException()
    }
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
        else  -> errIntOverflow()
    }
}

private fun Long.checkOverflowMinus(other: Long): Number {
    // uses XOR for a similar logic than plus

    val result: Long = this - other
    val overflows = ((this xor other) < 0) and ((this xor result) < 0)
    return when (overflows) {
        false -> result
        else  -> errIntOverflow()
    }
}

private fun Long.checkOverflowTimes(other: Long): Number {
    fun Long.numberOfLeadingZeros() = java.lang.Long.numberOfLeadingZeros(this)

    // Hacker's Delight, Section 2-12

    val leadingZeros = this.numberOfLeadingZeros() +
                       this.inv().numberOfLeadingZeros() +
                       other.numberOfLeadingZeros() +
                       other.inv().numberOfLeadingZeros()

    val result = this * other
    val longSize = java.lang.Long.SIZE

    if ((leadingZeros >= longSize) &&
        ((this >= 0) or (other != Long.MIN_VALUE)) &&
        (this == 0L || result / this == other)) {
        return result
    }

    errIntOverflow()
}

private fun Long.checkOverflowDivision(other: Long): Number {
    // division can only underflow Long.MIN_VALUE / -1
    // because abs(Long.MIN_VALUE) == abs(Long.MAX_VALUE) + 1
    if(this == Long.MIN_VALUE && other == -1L){
        errIntOverflow()
    }

    return this / other
}

operator fun Number.plus(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.checkOverflowPlus(second as Long)
        is Double -> first + second as Double
        is BigDecimal -> first.add(second as BigDecimal, MATH_CONTEXT)
        else -> throw IllegalStateException()
    }
}

operator fun Number.minus(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.checkOverflowMinus(second as Long)
        is Double -> first - second as Double
        is BigDecimal -> first.subtract(second as BigDecimal, MATH_CONTEXT)
        else -> throw IllegalStateException()
    }
}

operator fun Number.times(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.checkOverflowTimes(second as Long)
        is Double -> first * second as Double
        is BigDecimal -> first.multiply(second as BigDecimal, MATH_CONTEXT)
        else -> throw IllegalStateException()
    }
}

operator fun Number.div(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.checkOverflowDivision(second as Long)
        is Double -> first / second as Double
        is BigDecimal -> first.divide(second as BigDecimal, MATH_CONTEXT)
        else -> throw IllegalStateException()
    }
}

operator fun Number.rem(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first % second as Long
        is Double -> first % second as Double
        is BigDecimal -> first.remainder(second as BigDecimal, MATH_CONTEXT)
        else -> throw IllegalStateException()
    }
}

operator fun Number.compareTo(other: Number): Int {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.compareTo(second as Long)
        is Double -> first.compareTo(second as Double)
        is BigDecimal -> first.compareTo(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}

val Number.isNaN get() = when(this) {
    is Double -> isNaN()
    else -> false
}

val Number.isNegInf get() = when(this) {
    is Double -> isInfinite() && this < 0
    else -> false
}

val Number.isPosInf get() = when(this) {
    is Double -> isInfinite() && this > 0
    else -> false
}
