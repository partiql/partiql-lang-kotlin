/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.util

import com.amazon.ion.*
import java.math.*

private val CONVERSION_MAP = mapOf<Set<Class<*>>, Class<out Number>>(
    setOf(Long::class.javaObjectType, Long::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Long::class.javaObjectType, BigInteger::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(Long::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Long::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,

    setOf(BigInteger::class.javaObjectType, BigInteger::class.javaObjectType) to BigInteger::class.javaObjectType,
    setOf(BigInteger::class.javaObjectType, Double::class.javaObjectType) to BigDecimal::class.javaObjectType,
    setOf(BigInteger::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,

    setOf(Double::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,

    setOf(BigDecimal::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType)

private val CONVERTERS = mapOf<Class<*>, (Number) -> Number>(
    Long::class.javaObjectType      to Number::toLong,
    BigInteger::class.java          to { num ->
        when(num) {
            is Long       -> BigInteger.valueOf(num)
            is BigInteger -> num
            is Double     -> BigInteger.valueOf(num.toLong())
            is BigDecimal -> num.toBigInteger()
            else          -> throw IllegalArgumentException("Unsupported number for decimal conversion: $num")
        }
    },
    Double::class.javaObjectType    to Number::toDouble,
    BigDecimal::class.java to { num ->
        when (num) {
            is Long       -> BigDecimal.valueOf(num)
            is BigInteger -> BigDecimal(num)
            is Double     -> BigDecimal.valueOf(num)
            is BigDecimal -> num
            else          -> throw IllegalArgumentException("Unsupported number for decimal conversion: $num")
        }
    }
)


internal fun Number.isZero() = when(this) {
    // using compareTo instead of equals for BigDecimal and BigInteger because equality also checks same scale

    is Long -> this == 0L
    is BigInteger -> this.compareTo(BigInteger.ZERO) == 0
    is Double -> this == 0.0
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
    val type = CONVERSION_MAP[setOf(first.javaClass, second.javaClass)] ?:
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
        is BigDecimal -> this.negate()
        else -> throw IllegalStateException()
    }
}

private fun Long.nonOverflowingPlus(other: Long): Number {
    // uses to XOR to check if
    // this and other are >= 0 then if result < 0 means overflow
    // this and other are < 0 then if result > 0 means overflow
    // if this and other have different signs then no overflow can happen

    val result: Long = this + other
    val overflows = ((this xor other) >= 0) and ((this xor result) < 0)
    return when (overflows) {
        false -> result
        else  -> BigInteger.valueOf(this).add(BigInteger.valueOf(other))
    }
}

private fun Long.nonOverflowingMinus(other: Long): Number {
    // uses XOR for a similar logic than plus

    val result: Long = this - other
    val overflows = ((this xor other) < 0) and ((this xor result) < 0)
    return when (overflows) {
        false -> result
        else  -> BigInteger.valueOf(this).minus(BigInteger.valueOf(other))
    }
}

private fun Long.nonOverflowingTimes(other: Long): Number {
    fun Long.numberOfLeadingZeros() = java.lang.Long.numberOfLeadingZeros(this)

    // Hacker's Delight, Section 2-12

    val leadingZeros = this.numberOfLeadingZeros() +
                       this.inv().numberOfLeadingZeros() +
                       other.numberOfLeadingZeros() +
                       other.inv().numberOfLeadingZeros()

    val result = this * other
    val longSize = java.lang.Long.SIZE

    if((leadingZeros >= longSize) &&
       ((this >= 0) or (other != Long.MIN_VALUE)) &&
       (this == 0L || result / this == other)){
        return result
    }

    return BigInteger.valueOf(this).times(BigInteger.valueOf(other))
}

operator fun Number.plus(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.nonOverflowingPlus(second as Long)
        is BigInteger -> first.add(second as BigInteger)
        is Double -> first + second as Double
        is BigDecimal -> first.add(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}

operator fun Number.minus(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.nonOverflowingMinus(second as Long)
        is BigInteger -> first.subtract(second as BigInteger)
        is Double -> first - second as Double
        is BigDecimal -> first.subtract(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}

operator fun Number.times(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.nonOverflowingTimes(second as Long)
        is BigInteger -> first.multiply(second as BigInteger)
        is Double -> first * second as Double
        is BigDecimal -> first.multiply(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}

operator fun Number.div(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first / second as Long
        is BigInteger -> first.divide(second as BigInteger)
        is Double -> first / second as Double
        is BigDecimal -> first.divide(second as BigDecimal, MathContext.DECIMAL128) // TODO should this be configurable?
        else -> throw IllegalStateException()
    }
}

operator fun Number.rem(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first % second as Long 
        is BigInteger -> first.remainder(second as BigInteger)
        is Double -> first % second as Double
        is BigDecimal -> first.remainder(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}

operator fun Number.compareTo(other: Number): Int {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first.compareTo(second as Long)
        is BigInteger -> first.compareTo(second as BigInteger)
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
