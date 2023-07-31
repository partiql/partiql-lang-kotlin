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

import com.amazon.ion.Decimal
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import org.partiql.errors.ErrorCode
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.errIntOverflow
import org.partiql.lang.eval.errNoContext
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

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
    is Double -> BigDecimal(num, mc)
    is BigDecimal -> num
    Decimal.NEGATIVE_ZERO -> num as Decimal
    else -> throw IllegalArgumentException("Unsupported number type: $num, ${num.javaClass}")
}

internal fun bigDecimalOf(text: String, mc: MathContext = MATH_CONTEXT): BigDecimal = BigDecimal(text.trim(), mc)

private val CONVERSION_MAP = mapOf<Set<Class<*>>, Class<out Number>>(
    setOf(Long::class.javaObjectType, Long::class.javaObjectType) to Long::class.javaObjectType,
    setOf(Long::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Long::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,

    setOf(Double::class.javaObjectType, Double::class.javaObjectType) to Double::class.javaObjectType,
    setOf(Double::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType,

    setOf(BigDecimal::class.javaObjectType, BigDecimal::class.javaObjectType) to BigDecimal::class.javaObjectType
)

private val CONVERTERS = mapOf<Class<*>, (Number) -> Number>(
    Long::class.javaObjectType to Number::toLong,
    Double::class.javaObjectType to Number::toDouble,
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

internal fun Number.isZero() = when (this) {
    // using compareTo instead of equals for BigDecimal because equality also checks same scale

    is Long -> this == 0L
    is Double -> this == 0.0 || this == -0.0
    is BigDecimal -> BigDecimal.ZERO.compareTo(this) == 0
    else -> throw IllegalStateException()
}

@Suppress("UNCHECKED_CAST")
/** Provides a narrowing or widening operator on supported numbers. */
fun <T> Number.coerce(type: Class<T>): T where T : Number {
    val conv = CONVERTERS[type] ?: throw IllegalArgumentException("No converter for $type")
    return conv(this) as T
}

/**
 * Implements a very simple number tower to convert two numbers to their arithmetic
 * compatible type.
 *
 * This is only supported on limited types needed by the expression system.
 */
fun coerceNumbers(first: Number, second: Number): Pair<Number, Number> {
    fun typeFor(n: Number): Class<*> = if (n is Decimal) {
        BigDecimal::class.javaObjectType
    } else {
        n.javaClass
    }

    val type = CONVERSION_MAP[setOf(typeFor(first), typeFor(second))]
        ?: throw IllegalArgumentException("No coercion support for ${typeFor(first)} to ${typeFor(second)}")

    return Pair(first.coerce(type), second.coerce(type))
}

fun Number.ionValue(ion: IonSystem): IonValue = when (this) {
    is Long -> ion.newInt(this)
    is BigInteger -> ion.newInt(this)
    is Double -> ion.newFloat(this)
    is BigDecimal -> ion.newDecimal(this)
    else -> throw IllegalArgumentException("Cannot convert to IonValue: $this")
}

internal fun Number.exprValue(): ExprValue = when (this) {
    is Int -> ExprValue.newInt(this)
    is Long -> ExprValue.newInt(this)
    is Double -> ExprValue.newFloat(this)
    is BigDecimal -> ExprValue.newDecimal(this)
    else -> errNoContext(
        "Cannot convert number to expression value: $this",
        errorCode = ErrorCode.EVALUATOR_INVALID_CONVERSION,
        internal = true
    )
}

operator fun Decimal.unaryMinus(): Decimal = when {
    isZero() -> Decimal.negativeZero(this.scale())
    else -> Decimal.valueOf(negate())
}

operator fun Number.unaryMinus(): Number {
    return when (this) {
        // - LONG.MIN_VALUE will result in LONG.MIN_VALUE in JVM because LONG is a signed two's-complement integers
        is Long -> if (this == Long.MIN_VALUE) BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE) else -this
        is BigInteger -> this.negate()
        is Double -> -this
        is BigDecimal -> if (this.isZero()) {
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
        else -> errIntOverflow(8)
    }
}

private fun Long.checkOverflowMinus(other: Long): Number {
    // uses XOR for a similar logic than plus

    val result: Long = this - other
    val overflows = ((this xor other) < 0) and ((this xor result) < 0)
    return when (overflows) {
        false -> result
        else -> errIntOverflow(8)
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
        (this == 0L || result / this == other)
    ) {
        return result
    }

    errIntOverflow(8)
}

private fun Long.checkOverflowDivision(other: Long): Number {
    // division can only underflow Long.MIN_VALUE / -1
    // because abs(Long.MIN_VALUE) == abs(Long.MAX_VALUE) + 1
    if (this == Long.MIN_VALUE && other == -1L) {
        errIntOverflow(8)
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

val Number.isNaN
    get() = when (this) {
        is Double -> isNaN()
        else -> false
    }

val Number.isNegInf
    get() = when (this) {
        is Double -> isInfinite() && this < 0
        else -> false
    }

val Number.isPosInf
    get() = when (this) {
        is Double -> isInfinite() && this > 0
        else -> false
    }

/**
 * Returns the given BigDecimal with precision equals to mathContext.precision.
 *
 * This is for formatting purpose, all the digit that we are supposedly saying is correct will be shown.
 */
private fun BigDecimal.roundToDigits(mathContext: MathContext): BigDecimal {
    val stripped = this.stripTrailingZeros()
    val scale = stripped.scale() - stripped.precision() + 1
    val mantissa = stripped.scaleByPowerOfTen(scale)
    return if (mantissa.precision() != mathContext.precision) {
        mantissa.round(mathContext).setScale(mathContext.precision - 1).scaleByPowerOfTen(-scale)
    } else {
        stripped.round(mathContext)
    }
}

/**
 * Computes the nth root of a given BigDecimal x.
 * where x needs to be positive integer.
 */
private fun BigDecimal.intRoot(
    root: Int,
    mathContext: MathContext
): BigDecimal {
    if (this.signum() < 0) {
        throw ArithmeticException("Cannot take root of a negative number")
    }

    val operationMC = MathContext(
        if (mathContext.precision + 2 < 0) Int.MAX_VALUE else mathContext.precision + 2,
        mathContext.roundingMode
    )

    val tolerance: BigDecimal = BigDecimal.valueOf(5L).movePointLeft(mathContext.precision + 1)

    // using Newton's method
    // x_i = ( (n-1) (x_i-1) ^ n + a) / n (x_i-1) ^(n-1)
    // where a is the number whose nth root we want to compute
    val n = BigDecimal.valueOf(root.toLong())
    val nMinusOne = BigDecimal.valueOf(root.toLong() - 1L)

    // The initial approximation is x/n.
    var res = this.divide(n, mathContext)

    var resPrev: BigDecimal
    do {
        // x^(n-1)
        val xToNMinusOne = res.pow(root - 1, operationMC)
        // x^n
        val xToN = res.multiply(xToNMinusOne, operationMC)

        // n + (n-1)*(x^n)
        val numerator = this.add(nMinusOne.multiply(xToN, operationMC), operationMC)

        // (n*(x^(n-1))
        val denominator = n.multiply(xToNMinusOne, operationMC)

        // x = (n + (n-1)*(x^n)) / (n*(x^(n-1)))
        resPrev = res

        res = numerator.divide(denominator, operationMC)
    } while (res.round(mathContext).subtract(resPrev.round(mathContext)).abs() > tolerance)
    return res
}

/**
 * Computes the square root of a given BigDecimal.
 * See https://dl.acm.org/doi/pdf/10.1145/214408.214413
 */
fun BigDecimal.squareRoot(mathContext: MathContext = MATH_CONTEXT): BigDecimal {
    if (this.signum() < 0) {
        throw ArithmeticException("Cannot take root of a negative number")
    }

    // Special case:
    if (this.signum() == 0) {
        return BigDecimal.ZERO.roundToDigits(mathContext)
    }

    // We want to utilize the floating number's sqrt method to take an educated guess
    // to make sure the number is representable
    // we operate on normalized mantissa, which is [0.1, 10)
    val stripped = this.stripTrailingZeros()
    val scale = stripped.scale() - stripped.precision() + 1
    val scaleAdj = if (scale % 2 == 0) scale else scale - 1
    val mantissa = stripped.scaleByPowerOfTen(scaleAdj)

    val guess = BigDecimal.valueOf(kotlin.math.sqrt(mantissa.toDouble()))

    // Conservative guess of the precision of the result of a floating point calculation.
    var guessPrecision = 10

    // we need this additional logic in case of overflow
    val targetPrecision = mathContext.precision
    val normalizedPrecision = mantissa.precision()
    var approx = guess

    val zeroPointFive = BigDecimal.ONE.divide(BigDecimal.valueOf(2))
    do {
        // plus 2 for precision buffering
        val operatingPrecision = kotlin.math.max(
            kotlin.math.max(guessPrecision, targetPrecision + 2),
            normalizedPrecision
        )
        val tempMC = MathContext(operatingPrecision, RoundingMode.HALF_EVEN)
        approx = zeroPointFive.multiply(approx.add(mantissa.divide(approx, tempMC), tempMC))
        // the magic number here is 2p + 2, consider precision(x*x) is maxed at precision(x) + precision(x)
        guessPrecision = 2 * guessPrecision + 2
    } while (guessPrecision < targetPrecision + 2)

    // scale modification
    val unModifiedRes = approx.scaleByPowerOfTen(-scaleAdj / 2).round(mathContext)

    return unModifiedRes.roundToDigits(mathContext)
}

/**
 * Computes e^x of a given BigDecimal x.
 */
fun BigDecimal.exp(mathContext: MathContext = MATH_CONTEXT): BigDecimal {
    val operationMC = MathContext(
        if (10 + mathContext.precision < 0) Int.MAX_VALUE else 10 + mathContext.precision,
        mathContext.roundingMode
    )
    return if (this.signum() == 0) {
        BigDecimal.ONE.roundToDigits(mathContext)
    } else if (this.signum() == -1) {
        val reciprocal = this.negate().expHelper(operationMC)
        BigDecimal.valueOf(1)
            .divide(
                reciprocal,
                operationMC
            ).roundToDigits(mathContext)
    } else {
        this.expHelper(operationMC).roundToDigits(mathContext)
    }
}

/**
 * Computes the exponential value of a BigDecimal.
 */
private fun BigDecimal.expHelper(mathContext: MathContext): BigDecimal {
    // For faster convergence, we break exponent into integer and fraction parts.
    // e^x = e^(i+f) = (e^(1+f/i)) ^i
    // 1 + f/i < 2
    var intPart = this.setScale(0, RoundingMode.DOWN)

    if (intPart.signum() == 0) {
        return this.expTaylor(mathContext)
    }

    val fractionPart = this.subtract(intPart)
    // 1 + f/i
    val expInner = BigDecimal.ONE
        .add(
            fractionPart.divide(
                intPart, mathContext
            )
        )

    // e^(1+f/i)
    val etoExpInner = expInner.expTaylor(mathContext)
    // The build in power function can only handle int type, which max out at 999999999
    val maxInt = BigDecimal.valueOf(999999999L)
    var result = BigDecimal.ONE

    while (intPart >= maxInt) {
        result = result.multiply(
            etoExpInner.pow(999999999, mathContext),
        )
        intPart = intPart.subtract(maxInt)
    }
    return result.multiply(etoExpInner.pow(intPart.toInt(), mathContext), mathContext)
}

/**
 * Taylor series: e^x = 1 + x + 1/2!x^2 + .....
 */
private fun BigDecimal.expTaylor(mathContext: MathContext): BigDecimal {

    var factorial = BigDecimal.ONE
    var xToN = this
    var sumPrev: BigDecimal?

    var sum = this.add(BigDecimal.ONE)

    var i = 2

    do {
        xToN = xToN.multiply(this, mathContext)

        factorial = factorial.multiply(BigDecimal.valueOf(i.toLong()), mathContext)

        // x^n/factory
        val term = xToN
            .divide(
                factorial,
                mathContext
            )

        sumPrev = sum.round(mathContext)

        sum = sum.add(term, mathContext)
        i += 1
    } while (sum != sumPrev)
    return sum
}

/**
 * Compute the natural logarithm of a big decimal.
 */
fun BigDecimal.ln(mathContext: MathContext = MATH_CONTEXT): BigDecimal {
    if (this.signum() <= 0) {
        throw ArithmeticException("Cannot take natural log of a non-positive number")
    }
    if (this.compareTo(BigDecimal.ONE) == 0) {
        return BigDecimal.ZERO.roundToDigits(MATH_CONTEXT)
    }
    val intPart = this.setScale(0, RoundingMode.DOWN)
    val intPartLength = intPart.precision()
    val operationMC = MathContext(
        if (10 + mathContext.precision < 0) Int.MAX_VALUE else 10 + mathContext.precision,
        mathContext.roundingMode
    )
    // For faster converge, we calculate m*ln(root(x,m)) for m >= 3.
    return if (intPartLength < 3) {
        this.lnNewton(operationMC).roundToDigits(mathContext)
    } else {
        val root = this.intRoot(intPartLength, operationMC)
        val lnRoot = root.lnNewton(operationMC)
        val unModifiedRes = BigDecimal.valueOf(intPartLength.toLong()).multiply(lnRoot, operationMC)
        unModifiedRes.roundToDigits(mathContext)
    }
}

/**
 * Newton's method to compute natural log
 */
private fun BigDecimal.lnNewton(mathContext: MathContext): BigDecimal {
    val operationMC = MathContext(
        if (mathContext.precision + 2 < 0) Int.MAX_VALUE else mathContext.precision + 2,
        mathContext.roundingMode
    )

    val tolerance: BigDecimal = BigDecimal.valueOf(5L).movePointLeft(mathContext.precision + 1)

    // x_i = x_i-1 - (e^x_i-1 - n) / e^x_i-1
    var x = this
    val n = this
    var term: BigDecimal

    do {
        val eToX = x.expHelper(operationMC)
        term = eToX.subtract(n)
            .divide(eToX, operationMC)
        x = x.subtract(term)
    } while (term > tolerance)
    return x
}

/**
 * Calculate the given big decimal raised to the pth power, where p is another big decimal.
 */
fun BigDecimal.power(
    power: BigDecimal,
    mathContext: MathContext = MATH_CONTEXT
): BigDecimal {
    val operationMC = MathContext(
        if (10 + mathContext.precision < 0) Int.MAX_VALUE else 10 + mathContext.precision,
        mathContext.roundingMode
    )

    // x^(p) = x^(i + f) = x^i * x^f
    var intPart = power.setScale(0, RoundingMode.DOWN)
    val fractionPart = power.subtract(intPart)

    if (fractionPart.compareTo(BigDecimal.ZERO) != 0 && this < BigDecimal.ZERO) {
        throw ArithmeticException("a negative number raised to a non-integer power yields a complex result")
    }

    val maxInt = BigDecimal.valueOf(999999999L)
    var result = BigDecimal.ONE

    while (intPart >= maxInt) {
        result = result.multiply(
            this.pow(999999999, operationMC),
            operationMC
        )
        intPart = intPart.subtract(maxInt)
    }

    // x^i
    result = result.multiply(
        this.pow(intPart.toInt(), operationMC),
        operationMC
    )

    // x^f = exp(f*ln(x)) ;
    return if (fractionPart.compareTo(BigDecimal.ZERO) != 0) {
        val lnX = this.ln(operationMC)
        val fTimesLnX: BigDecimal = fractionPart.multiply(lnX, operationMC)
        val xToF = fTimesLnX.exp(operationMC)
        result.multiply(xToF, operationMC).roundToDigits(mathContext)
    } else {
        result.roundToDigits(mathContext)
    }
}
