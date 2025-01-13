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

package org.partiql.spi.function.builtins.internal

import com.amazon.ion.Decimal
import org.partiql.spi.errors.TypeCheckException
import org.partiql.spi.function.Aggregation
import org.partiql.spi.internal.coerceNumbers
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

internal abstract class Accumulator : Aggregation.Accumulator {

    override fun next(args: Array<Datum>) {
        val value = args[0]
        if (value.isNull || value.isMissing) return
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

internal fun checkIsNumberType(funcName: String, value: Datum) {
    if (value.type.code() == PType.VARIANT) {
        return checkIsNumberType(funcName, value.lower())
    }
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
        is Int -> first / second as Int
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
    if (value.type.code() == PType.VARIANT) {
        return checkIsBooleanType(funcName, value.lower())
    }
    if (value.type.code() != PType.BOOL) {
        throw TypeCheckException("Expected ${PType.BOOL} but received ${value.type}.")
    }
}

internal fun Datum.numberValue(): Number = when (this.type.code()) {
    PType.VARIANT -> this.lower().numberValue()
    PType.TINYINT -> this.byte
    PType.SMALLINT -> this.short
    PType.INTEGER -> this.int
    PType.BIGINT -> this.long
    PType.NUMERIC -> this.bigDecimal
    PType.REAL -> this.float
    PType.DOUBLE -> this.double
    PType.DECIMAL -> this.bigDecimal
    else -> error("Cannot convert PartiQLValue ($this) to number.")
}

internal fun Datum.booleanValue(): Boolean = when (this.type.code()) {
    PType.VARIANT -> this.lower().booleanValue()
    PType.BOOL -> this.boolean
    else -> error("Cannot convert PartiQLValue ($this) to boolean.")
}

internal fun PType.isNumber(): Boolean = when (this.code()) {
    PType.INTEGER,
    PType.TINYINT,
    PType.SMALLINT,
    PType.BIGINT,
    PType.NUMERIC,
    PType.REAL,
    PType.DOUBLE,
    PType.DECIMAL,
    -> true
    else -> false
}

/**
 * This is specifically for SUM/AVG
 */
internal fun nullToTargetType(type: PType): Datum = Datum.nullValue(type)

/**
 * This is specifically for SUM/AVG
 */
internal fun Number.toTargetType(type: PType): Datum = when (type.code()) {
    PType.DYNAMIC -> this.toDatum()
    PType.REAL -> Datum.real(this.toFloat())
    PType.DOUBLE -> Datum.doublePrecision(this.toDouble())
    PType.DECIMAL -> {
        when (this) {
            is BigDecimal -> Datum.decimal(this, this.precision(), this.scale())
            is BigInteger -> {
                val d = this.toBigDecimal()
                Datum.decimal(d, d.precision(), d.scale())
            }
            else -> {
                val d = BigDecimal.valueOf(this.toDouble())
                Datum.decimal(d, d.precision(), d.scale())
            }
        }
    }
    PType.TINYINT -> Datum.tinyint(this.toByte())
    PType.SMALLINT -> Datum.smallint(this.toShort())
    PType.INTEGER -> Datum.integer(this.toInt())
    PType.BIGINT -> Datum.bigint(this.toLong())
    PType.NUMERIC -> when (this) {
        is BigInteger -> Datum.numeric(this.toBigDecimal())
        is BigDecimal -> Datum.numeric(this)
        else -> Datum.numeric(BigDecimal.valueOf(this.toLong()))
    }
    else -> TODO("Unsupported target type $type")
}

internal fun Number.toDatum(): Datum = when (this) {
    is Int -> Datum.integer(this)
    is Long -> Datum.bigint(this)
    is Double -> Datum.doublePrecision(this)
    is BigDecimal -> Datum.decimal(this, this.precision(), this.scale())
    is BigInteger -> Datum.numeric(this.toBigDecimal())
    else -> TODO("Could not convert $this to PartiQL Value")
}

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
