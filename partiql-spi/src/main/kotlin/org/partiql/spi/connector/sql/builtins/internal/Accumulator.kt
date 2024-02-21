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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.spi.connector.sql.builtins.internal

import com.amazon.ion.Decimal
import org.partiql.errors.TypeCheckException
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.BoolValue
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.decimalValue
import org.partiql.value.float64Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.intValue
import org.partiql.value.util.coerceNumbers
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

@OptIn(FnExperimental::class)
internal abstract class Accumulator : Agg.Accumulator {

    /** Accumulates the next value into this [Accumulator]. */
    @OptIn(PartiQLValueExperimental::class)
    override fun next(args: Array<PartiQLValue>) {
        val value = args[0]
        if (value.isUnknown()) return
        nextValue(value)
    }

    abstract fun nextValue(value: PartiQLValue)
}

// TODO: Make this better
@OptIn(PartiQLValueExperimental::class)
internal fun comparisonAccumulator(comparator: Comparator<PartiQLValue>): (PartiQLValue?, PartiQLValue) -> PartiQLValue =
    { left, right ->
        when {
            left == null || comparator.compare(left, right) > 0 -> right
            else -> left
        }
    }

// TODO: Make this better
@OptIn(PartiQLValueExperimental::class)
internal fun checkIsNumberType(funcName: String, value: PartiQLValue) {
    if (!value.type.isNumber()) {
        throw TypeCheckException("Expected NUMBER but received ${value.type}.")
    }
}

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

// TODO: Make this better
@OptIn(PartiQLValueExperimental::class)
internal fun checkIsBooleanType(funcName: String, value: PartiQLValue) {
    if (value.type != PartiQLValueType.BOOL) {
        throw TypeCheckException("Expected ${PartiQLValueType.BOOL} but received ${value.type}.")
    }
}

// TODO: Make this better
@OptIn(PartiQLValueExperimental::class)
internal fun PartiQLValue.isUnknown(): Boolean = this.type == PartiQLValueType.MISSING || this.isNull

// TODO: Make this better
@OptIn(PartiQLValueExperimental::class)
internal fun PartiQLValue.numberValue(): Number = when (this) {
    is IntValue -> this.value!!
    is Int8Value -> this.value!!
    is Int16Value -> this.value!!
    is Int32Value -> this.value!!
    is Int64Value -> this.value!!
    is DecimalValue -> this.value!!
    is Float32Value -> this.value!!
    is Float64Value -> this.value!!
    else -> error("Cannot convert PartiQLValue ($this) to number.")
}

// TODO: Make this better
@OptIn(PartiQLValueExperimental::class)
internal fun PartiQLValue.booleanValue(): Boolean = when (this) {
    is BoolValue -> this.value!!
    else -> error("Cannot convert PartiQLValue ($this) to boolean.")
}

// TODO: Make this better
@OptIn(PartiQLValueExperimental::class)
internal fun PartiQLValueType.isNumber(): Boolean = when (this) {
    PartiQLValueType.INT,
    PartiQLValueType.INT8,
    PartiQLValueType.INT16,
    PartiQLValueType.INT32,
    PartiQLValueType.INT64,
    PartiQLValueType.DECIMAL,
    PartiQLValueType.DECIMAL_ARBITRARY,
    PartiQLValueType.FLOAT32,
    PartiQLValueType.FLOAT64 -> true
    else -> false
}

// TODO: Make this better
@OptIn(PartiQLValueExperimental::class)
internal fun Number.partiqlValue(): PartiQLValue = when (this) {
    is Int -> int32Value(this)
    is Long -> int64Value(this)
    is Double -> float64Value(this)
    is BigDecimal -> decimalValue(this)
    is BigInteger -> intValue(this)
    else -> TODO("Error context")
}

// TODO: Make this better
private val MATH_CONTEXT = MathContext(38, RoundingMode.HALF_EVEN)

// TODO: Make this better
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
