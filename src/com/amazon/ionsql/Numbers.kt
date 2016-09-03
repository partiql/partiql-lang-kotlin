/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import java.math.BigDecimal

private val RANKS = mapOf(
    Long::class.java        to 1,
    Double::class.java      to 2,
    BigDecimal::class.java  to 3
)

private val CONVERTERS = mapOf<Class<*>, (Number) -> Number>(
    Long::class.java        to Number::toLong,
    Double::class.java      to Number::toDouble,
    BigDecimal::class.java  to { num ->
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

fun Number.coerce(type: Class<Number>): Number {
    val conv = CONVERTERS[type] ?: throw IllegalArgumentException("No converter for $type")
    return conv(this)
}

fun coerceNumbers(first: Number, second: Number): Pair<Number, Number> {
    val type = listOf(first.javaClass, second.javaClass).maxBy { RANKS[it]!! }!!

    return Pair(first.coerce(type), second.coerce(type))
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
        is BigDecimal -> first.divide(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}

operator fun Number.mod(other: Number): Number {
    val (first, second) = coerceNumbers(this, other)
    return when (first) {
        is Long -> first % second as Long
        is Double -> first % second as Double
        is BigDecimal -> first.remainder(second as BigDecimal)
        else -> throw IllegalStateException()
    }
}