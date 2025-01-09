package org.partiql.spi.internal

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

import com.amazon.ion.Decimal
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/**
 * Essentially the same as partiql-lang's NumberExtensions.kt. Key differences are the following:
 * - Adding [Int] and [Float] branch when casing on the [Number]
 * - TODO support for integer types smaller than [Int]
 */

// TODO should this be configurable?
private val MATH_CONTEXT = MathContext(38, RoundingMode.HALF_EVEN)

/**
 * Factory function to create a [BigDecimal] using correct precision, use it in favor of native BigDecimal constructors
 * and factory methods
 */
internal fun bigDecimalOf(num: Number, mc: MathContext = MATH_CONTEXT): BigDecimal = when (num) {
    is Decimal -> num
    is Int -> BigDecimal(num, mc)
    is Long -> BigDecimal(num, mc)
    is Float -> BigDecimal(num.toDouble(), mc)
    is Double -> BigDecimal(num, mc)
    is BigInteger -> BigDecimal(num, mc)
    is BigDecimal -> num
    is BigInteger -> num.toBigDecimal()
    else -> throw IllegalArgumentException("Unsupported number type: $num, ${num.javaClass}")
}

/**
 * This should handle Byte, Short, Int, Long, BigInteger, Float, Double, BigDecimal
 */
private val CONVERSION_MAP = mapOf<Set<Class<*>>, Class<out Number>>(
    // BYTE
    setOf(Byte::class.javaObjectType, Byte::class.javaObjectType) to Byte::class.javaObjectType,
    setOf(Byte::class.javaObjectType, Short::class.javaObjectType) to Short::class.javaObjectType,
    setOf(Byte::class.javaObjectType, Int::class.javaObjectType) to Int::class.javaObjectType,
    setOf(Byte::class.javaObjectType, Long::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Byte::class.javaObjectType, BigInteger::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(Byte::class.javaObjectType, Float::class.javaObjectType) to Float::class.javaObjectType,
    setOf(Byte::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Byte::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,
    // SHORT
    setOf(Short::class.javaObjectType, Byte::class.javaObjectType) to Short::class.javaObjectType,
    setOf(Short::class.javaObjectType, Short::class.javaObjectType) to Short::class.javaObjectType,
    setOf(Short::class.javaObjectType, Int::class.javaObjectType) to Int::class.javaObjectType,
    setOf(Short::class.javaObjectType, Long::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Short::class.javaObjectType, BigInteger::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(Short::class.javaObjectType, Float::class.javaObjectType) to Float::class.javaObjectType,
    setOf(Short::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Short::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,
    // INT
    setOf(Int::class.javaObjectType, Byte::class.javaObjectType) to Int::class.javaObjectType,
    setOf(Int::class.javaObjectType, Short::class.javaObjectType) to Int::class.javaObjectType,
    setOf(Int::class.javaObjectType, Int::class.javaObjectType) to Int::class.javaObjectType,
    setOf(Int::class.javaObjectType, Long::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Int::class.javaObjectType, BigInteger::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(Int::class.javaObjectType, Float::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Int::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Int::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,
    // LONG
    setOf(Long::class.javaObjectType, Byte::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Long::class.javaObjectType, Short::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Long::class.javaObjectType, Int::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Long::class.javaObjectType, Long::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Long::class.javaObjectType, BigInteger::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(Long::class.javaObjectType, Float::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Long::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Long::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,
    // FLOAT
    setOf(Float::class.javaObjectType, Byte::class.javaObjectType) to Float::class.javaObjectType,
    setOf(Float::class.javaObjectType, Short::class.javaObjectType) to Float::class.javaObjectType,
    setOf(Float::class.javaObjectType, Int::class.javaObjectType) to Float::class.javaObjectType,
    setOf(Float::class.javaObjectType, Long::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Float::class.javaObjectType, BigInteger::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Float::class.javaObjectType, Float::class.javaObjectType) to Float::class.javaObjectType,
    setOf(Float::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Float::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,
    // DOUBLE
    setOf(Double::class.javaObjectType, Byte::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, Short::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, Int::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, Long::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, BigInteger::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, Float::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,
    // BIG INTEGER
    setOf(BigInteger::class.javaObjectType, Byte::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(BigInteger::class.javaObjectType, Short::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(BigInteger::class.javaObjectType, Int::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(BigInteger::class.javaObjectType, Long::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(BigInteger::class.javaObjectType, BigInteger::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(BigInteger::class.javaObjectType, Float::class.javaObjectType) to Double::class.javaObjectType,
    setOf(BigInteger::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(BigInteger::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,
    // BIG DECIMAL
    setOf(BigDecimal::class.javaObjectType, Byte::class.javaObjectType) to BigDecimal::class.javaObjectType,
    setOf(BigDecimal::class.javaObjectType, Short::class.javaObjectType) to BigDecimal::class.javaObjectType,
    setOf(BigDecimal::class.javaObjectType, Int::class.javaObjectType) to BigDecimal::class.javaObjectType,
    setOf(BigDecimal::class.javaObjectType, Long::class.javaObjectType) to BigDecimal::class.javaObjectType,
    setOf(BigDecimal::class.javaObjectType, BigInteger::class.javaObjectType) to BigDecimal::class.javaObjectType,
    setOf(BigDecimal::class.javaObjectType, Float::class.javaObjectType) to BigDecimal::class.javaObjectType,
    setOf(BigDecimal::class.javaObjectType, Double::class.javaObjectType) to BigDecimal::class.javaObjectType,
    setOf(BigDecimal::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,
)

private val CONVERTERS = mapOf<Class<*>, (Number) -> Number>(
    Byte::class.javaObjectType to Number::toByte,
    Short::class.javaObjectType to Number::toShort,
    Int::class.javaObjectType to Number::toInt,
    Long::class.javaObjectType to Number::toLong,
    Float::class.javaObjectType to Number::toFloat,
    Double::class.javaObjectType to Number::toDouble,
    BigInteger::class.javaObjectType to { num ->
        when (num) {
            is Byte -> num.toInt().toBigInteger()
            is Short -> num.toInt().toBigInteger()
            is Int -> num.toBigInteger()
            is Long -> num.toBigInteger()
            is BigInteger -> num
            else -> throw IllegalArgumentException(
                "Unsupported number for BigInteger conversion: $num"
            )
        }
    },
    BigDecimal::class.java to { num ->
        when (num) {
            is Byte -> bigDecimalOf(num)
            is Short -> bigDecimalOf(num)
            is Int -> bigDecimalOf(num)
            is Long -> bigDecimalOf(num)
            is Float -> bigDecimalOf(num)
            is Double -> bigDecimalOf(num)
            is BigDecimal -> bigDecimalOf(num)
            is BigInteger -> bigDecimalOf(num)
            else -> throw IllegalArgumentException(
                "Unsupported number for decimal conversion: $num (${num.javaClass.simpleName})"
            )
        }
    }
)

internal fun Number.isZero() = when (this) {
    is Byte -> this.toInt() == 0
    is Short -> this.toInt() == 0
    is Int -> this == 0
    is Long -> this == 0L
    is Float -> this == 0.0f || this == -0.0f
    is Double -> this == 0.0 || this == -0.0
    is BigDecimal -> this.signum() == 0
    is BigInteger -> this.signum() == 0
    else -> throw IllegalStateException("$this (${this.javaClass.simpleName})")
}

@Suppress("UNCHECKED_CAST")
/** Provides a narrowing or widening operator on supported numbers. */
internal fun <T> Number.coerce(type: Class<T>): T where T : Number {
    val conv = CONVERTERS[type] ?: throw IllegalArgumentException("No converter for $type")
    return conv(this) as T
}

/**
 * Implements a very simple number tower to convert two numbers to their arithmetic
 * compatible type.
 *
 * This is only supported on limited types needed by the expression system.
 * TODO: Make no longer public.
 */
public fun coerceNumbers(first: Number, second: Number): Pair<Number, Number> {
    fun typeFor(n: Number): Class<*> = if (n is Decimal) {
        BigDecimal::class.javaObjectType
    } else {
        n.javaClass
    }

    val type = CONVERSION_MAP[setOf(typeFor(first), typeFor(second))]
        ?: throw IllegalArgumentException("No coercion support for ${typeFor(first)} to ${typeFor(second)}")

    return Pair(first.coerce(type), second.coerce(type))
}

internal operator fun Number.compareTo(other: Number): Int {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Byte -> first.compareTo(second as Byte)
        is Short -> first.compareTo(second as Short)
        is Int -> first.compareTo(second as Int)
        is Long -> first.compareTo(second as Long)
        is Float -> first.compareTo(second as Float)
        is Double -> first.compareTo(second as Double)
        is BigDecimal -> first.compareTo(second as BigDecimal)
        is BigInteger -> first.compareTo(second as BigInteger)
        else -> throw IllegalStateException()
    }
}

internal val Number.isNaN
    get() = when (this) {
        is Float -> isNaN()
        is Double -> isNaN()
        else -> false
    }

internal val Number.isNegInf
    get() = when (this) {
        is Float -> isInfinite() && this < 0
        is Double -> isInfinite() && this < 0
        else -> false
    }

internal val Number.isPosInf
    get() = when (this) {
        is Float -> isInfinite() && this > 0
        is Double -> isInfinite() && this > 0
        else -> false
    }
