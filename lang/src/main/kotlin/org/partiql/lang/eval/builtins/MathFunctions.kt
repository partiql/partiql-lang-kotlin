package org.partiql.lang.eval.builtins

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.errIntOverflow
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.coerceNumbers
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.exp
import org.partiql.lang.util.isNaN
import org.partiql.lang.util.isNegInf
import org.partiql.lang.util.isPosInf
import org.partiql.lang.util.ln
import org.partiql.lang.util.power
import org.partiql.lang.util.squareRoot
import java.lang.Double.NEGATIVE_INFINITY
import java.lang.Double.NaN
import java.lang.Double.POSITIVE_INFINITY
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.pow

/**
 * A place to keep supported mathematical functions. We are missing many in comparison to PostgresQL.
 * https://www.postgresql.org/docs/14/functions-math.html
 */
object MathFunctions {

    fun create(valueFactory: ExprValueFactory): List<ExprFunction> = listOf(
        UnaryNumeric("ceil", valueFactory) { ceil(it) },
        UnaryNumeric("ceiling", valueFactory) { ceil(it) },
        UnaryNumeric("floor", valueFactory) { floor(it) },
        UnaryNumeric("abs", valueFactory) { abs(it) },
        UnaryNumeric("sqrt", valueFactory) { sqrt(it) },
        UnaryNumeric("exp", valueFactory) { exp(it) },
        UnaryNumeric("ln", valueFactory) { ln(it) },
        BinaryNumeric("pow", valueFactory) { n, p -> pow(n, p) }
    )
}

/**
 * A convenience class to wrap `(Number) -> Number` as a PartiQL ExprFunction.
 *
 * @property identifier Symbol for the given function
 * @property valueFactory Used to create the output ExprValue
 * @property function Function to invoke for the given signature
 */
private class UnaryNumeric(
    private val identifier: String,
    private val valueFactory: ExprValueFactory,
    private val function: (Number) -> Number
) : ExprFunction {

    override val signature = FunctionSignature(
        identifier,
        listOf(StaticType.NUMERIC),
        returnType = StaticType.NUMERIC,
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        function.invoke(required.first().numberValue()).exprValue(valueFactory)
}

/**
 * A convenience class to wrap `(Number, Number) -> Number` as a PartiQL ExprFunction.
 *
 * @property identifier Symbol for the given function
 * @property valueFactory Used to create the output ExprValue
 * @property function Function to invoke for the given signature
 */
private class BinaryNumeric(
    private val identifier: String,
    private val valueFactory: ExprValueFactory,
    private val function: (Number, Number) -> Number
) : ExprFunction {

    override val signature = FunctionSignature(
        identifier,
        listOf(StaticType.NUMERIC, StaticType.NUMERIC),
        returnType = StaticType.NUMERIC,
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        function.invoke(required.first().numberValue(), required[1].numberValue()).exprValue(valueFactory)
}

private fun ceil(n: Number): Number = when (n) {
    POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN -> n
    // support for numbers that are larger than 64 bits.
    else -> transformIntType(bigDecimalOf(n).setScale(0, RoundingMode.CEILING).toBigIntegerExact())
}

private fun floor(n: Number): Number = when (n) {
    POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN -> n
    else -> transformIntType(bigDecimalOf(n).setScale(0, RoundingMode.FLOOR).toBigIntegerExact())
}

private fun abs(n: Number): Number = when (n) {
    is Long -> {
        if (n == Long.MIN_VALUE) {
            errIntOverflow(8)
        } else {
            kotlin.math.abs(n)
        }
    }
    is Double -> kotlin.math.abs(n)
    is Float -> kotlin.math.abs(n)
    is BigDecimal -> n.abs()
    else -> errNoContext(
        message = "Unknown number type",
        errorCode = ErrorCode.INTERNAL_ERROR,
        internal = true
    )
}

private fun sqrt(n: Number): Number {
    if (n < 0L) {
        errNoContext(
            "Cannot take root of a negative number",
            errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
            internal = false
        )
    }
    return when (n) {
        is Long -> kotlin.math.sqrt(n.toDouble())
        is Double -> kotlin.math.sqrt(n)
        is Float -> kotlin.math.sqrt(n)
        is BigDecimal -> n.squareRoot()
        else -> errNoContext(
            message = "Unknown number type",
            errorCode = ErrorCode.INTERNAL_ERROR,
            internal = true
        )
    }
}

private fun exp(n: Number): Number {
    return when (n) {
        is Long -> kotlin.math.exp(n.toDouble())
        is Double -> kotlin.math.exp(n)
        is Float -> kotlin.math.exp(n)
        is BigDecimal -> n.exp()
        else -> errNoContext(
            message = "Unknown number type",
            errorCode = ErrorCode.INTERNAL_ERROR,
            internal = true
        )
    }
}

private fun ln(n: Number): Number {
    if (n <= 0L) {
        errNoContext(
            "Cannot take root of a non-positive number",
            errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
            internal = false
        )
    }
    return when (n) {
        is Long -> kotlin.math.ln(n.toDouble())
        is Double -> kotlin.math.ln(n)
        is Float -> kotlin.math.ln(n)
        is BigDecimal -> n.ln()
        else -> errNoContext(
            message = "Unknown number type",
            errorCode = ErrorCode.INTERNAL_ERROR,
            internal = true
        )
    }
}

private fun pow(n: Number, p: Number): Number {
    val (first, second) = if (n.isPosInf || n.isNegInf || n.isNaN) {
        n to p.toDouble()
    } else if (p.isPosInf || p.isNegInf || p.isNaN) {
        n.toDouble() to p.toDouble()
    } else {
        coerceNumbers(n, p)
    }

    return when (first) {
        is Long -> first.toDouble().pow(second.toDouble())
        is Double -> {
            if (first < 0.0 && ((second as Double) % 1.0 != 0.0)) {
                errNoContext(
                    message = "a negative number raised to a non-integer power yields a complex result",
                    errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                    internal = false
                )
            }
            first.pow(second as Double)
        }
        is BigDecimal ->
            try {
                first.power(second as BigDecimal)
            } catch (e: Exception) {
                errNoContext(
                    message = e.message ?: "Arithmetic Error",
                    errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                    internal = false
                )
            }
        else -> throw IllegalStateException()
    }
}

// wrapper for transform function result to corresponding integer type
private fun transformIntType(n: BigInteger): Number = when (n) {
    in Int.MIN_VALUE.toBigInteger()..Int.MAX_VALUE.toBigInteger() -> n.toInt()
    in Long.MIN_VALUE.toBigInteger()..Long.MAX_VALUE.toBigInteger() -> n.toLong()
    /**
     * currently PariQL-lang-kotlin did not support integer value bigger than 64 bits.
     */
    else -> errIntOverflow(8)
}

// wrapper for converting result to expression value
private fun Number.exprValue(valueFactory: ExprValueFactory): ExprValue {
    return when (this) {
        is Int -> valueFactory.newInt(this)
        is Long -> valueFactory.newInt(this)
        is Float, is Double -> valueFactory.newFloat(this.toDouble())
        is BigDecimal -> valueFactory.newDecimal(this)
        else -> errNoContext(
            "Cannot convert number to expression value: $this",
            errorCode = ErrorCode.EVALUATOR_INVALID_CONVERSION,
            internal = true
        )
    }
}
