/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.util

import com.amazon.ion.*
import java.math.*

private val RANKS = mapOf(
    Long::class.javaObjectType        to 1,
    Double::class.javaObjectType      to 2,
    BigDecimal::class.javaObjectType  to 3
)

private val CONVERTERS = mapOf<Class<*>, (Number) -> Number>(
    Long::class.javaObjectType      to Number::toLong,
    Double::class.javaObjectType    to Number::toDouble,
    BigDecimal::class.java          to { num ->
        when (num) {
            is Long -> BigDecimal.valueOf(num)
            is Double -> BigDecimal.valueOf(num)
            is BigDecimal -> num
            else -> throw IllegalArgumentException(
                "Unsupported number for decimal conversion: $num"
            )
        }
    }
)

internal fun Number.isZero() = when(this) {
    is Long -> this == 0L
    is Double -> this == 0.0
    // using compareTo instead of equals because equality also checks same scale
    // so BigDecimal("0.0") != BigDecimal.ZERO
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
    val type = listOf(first.javaClass, second.javaClass)
        .maxBy { RANKS[it] ?: throw IllegalArgumentException("No coercion support for $it") }!!

    return Pair(first.coerce(type), second.coerce(type))
}

fun Number.ionValue(ion: IonSystem): IonValue = when (this) {
    is Long -> ion.newInt(this)
    is Double -> ion.newFloat(this)
    is BigDecimal -> ion.newDecimal(this)
    else -> throw IllegalArgumentException("Cannot convert to IonValue: $this")
}

operator fun Number.unaryMinus(): Number {
    return when (this) {
        is Long -> -this
        is Double -> -this
        is BigDecimal -> this.negate()
        else -> throw IllegalStateException()
    }
}

operator fun Number.plus(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first + second as Long
        is Double -> first + second as Double
        is BigDecimal -> first.add(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}

operator fun Number.minus(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first - second as Long
        is Double -> first - second as Double
        is BigDecimal -> first.subtract(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}

operator fun Number.times(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first * second as Long
        is Double -> first * second as Double
        is BigDecimal -> first.multiply(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}

operator fun Number.div(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first / second as Long
        is Double -> first / second as Double
        is BigDecimal -> first.divide(second as BigDecimal, MathContext.DECIMAL128) // TODO should this be configurable?
        else -> throw IllegalStateException()
    }
}

operator fun Number.rem(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first % second as Long
        is Double -> first % second as Double
        is BigDecimal -> first.remainder(second as BigDecimal)
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
