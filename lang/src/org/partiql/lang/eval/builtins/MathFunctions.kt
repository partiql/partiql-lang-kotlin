package org.partiql.lang.eval.builtins

import com.amazon.ion.Decimal
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.errIntOverflow
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.intValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.isZero
import org.partiql.lang.util.unaryMinus
import java.lang.Double.NEGATIVE_INFINITY
import java.lang.Double.NaN
import java.lang.Double.POSITIVE_INFINITY
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * A place to keep supported mathematical functions. We are missing many in comparison to PostgresQL.
 * https://www.postgresql.org/docs/14/functions-math.html
 */
object MathFunctions {

    fun create(valueFactory: ExprValueFactory): List<ExprFunction> = listOf(
        UnaryNumeric("ceil", valueFactory) { ceil(it) },
        UnaryNumeric("ceiling", valueFactory) { ceil(it) },
        UnaryNumeric("floor", valueFactory) { floor(it) },
        RoundFunction(valueFactory)
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
    private val function: (Number) -> Number,
) : ExprFunction {

    override val signature = FunctionSignature(
        identifier,
        listOf(StaticType.NUMERIC),
        returnType = StaticType.NUMERIC,
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        function.invoke(required.first().numberValue()).exprValue(valueFactory)
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

// wrapper for transform function result to corresponding integer type
private fun transformIntType(n: BigInteger): Number = when (n) {
    in Int.MIN_VALUE.toBigInteger()..Int.MAX_VALUE.toBigInteger() -> n.toInt()
    in Long.MIN_VALUE.toBigInteger()..Long.MAX_VALUE.toBigInteger() -> n.toLong()
    /**
     * currently kotlin-lang-ParitQL did not support integer value bigger than 64 bits.
     */
    else -> errIntOverflow(8)
}

// wrapper for converting result to expression value
private fun Number.exprValue(valueFactory: ExprValueFactory): ExprValue = when (this) {
    is Int -> valueFactory.newInt(this)
    is Long -> valueFactory.newInt(this)
    is Double, is Float -> valueFactory.newFloat(this.toDouble())
    is BigDecimal -> valueFactory.newDecimal(this)
    else -> errNoContext(
        "Cannot convert number to expression value: $this",
        errorCode = ErrorCode.EVALUATOR_INVALID_CONVERSION,
        internal = true
    )
}

/**
 * | source  | targetScale | output      | description|
 * | Integer | omitted/0   | Integer     | round to the nearest Integer
 * | Integer | > 0         | Decimal     | transform to Decimal
 * | Integer | < 0         | Integer     | round to [targetScale] decimal places
 * | Inexact | omitted/0   | Integer     | round to the nearest Integer
 * | Inexact | > 0         | Not allowed | allowing this potentially introduce accuracy loss
 * | Inexact | < 0         | Integer     | round to [targetScale] decimal places
 * | Exact   | omitted/0   | Integer     | round to the nearest Integer
 * | Exact   | > 0         | Integer     | round to [targetScale] decimal places
 * | Exact   | < 0         | Integer     | round to [targetScale] decimal places
 **/
internal class RoundFunction(val valueFactory: ExprValueFactory) : ExprFunction {
    override val signature = FunctionSignature(
        name = "round",
        requiredParameters = listOf(StaticType.NUMERIC),
        returnType = StaticType.NUMERIC,
        optionalParameter = StaticType.unionOf(StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.INT)
    )

    override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        return round(required.first().numberValue(), opt.intValue()).exprValue(valueFactory)
    }

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        return round(required.first().numberValue(), 0).exprValue(valueFactory)
    }

    private fun round(source: Number, targetScale: Int): Number =
        when (source) {
            POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN -> source
            is Int, is Long -> roundInt(source, targetScale)
            is Double, is Float -> roundInexact(source, targetScale)
            else -> roundExact(source, targetScale)
        }

    private fun roundInt(source: Number, targetScale: Int): Number = when (targetScale) {
        // target scale is 0, no need to round
        0 -> source
        // target source is less than 0, we round this to integer
        in -Int.MAX_VALUE..-1 -> transformIntType(roundToDecimal(source, targetScale).toBigIntegerExact())
        // target source is bigger than 0
        // not really rounding this, just transform it to decimal with required scale
        else -> bigDecimalOf(source).setScale(targetScale)
    }

    private fun roundInexact(source: Number, targetScale: Int) = when (targetScale) {
        // target scale is 0, we around this to Integer
        0 -> transformIntType(roundToDecimal(source, targetScale).toBigIntegerExact())
        // target source is less than 0, we round this to integer
        in -Int.MAX_VALUE..-1 -> transformIntType(roundToDecimal(source, targetScale).toBigIntegerExact())
        // target source is bigger than 0, we do not round this because it may introduce accuracy issues if target scale is higher than the original accuracy
        else -> errNoContext(
            message = "Round Function with specified target scale other than 0 requires the source to be exact",
            // should I go with "FEATURE NOT SUPPORT YET"?
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
            internal = true
        )
    }

    private fun roundExact(source: Number, targetScale: Int) = when (targetScale) {
        // target scale is 0, we around this to Integer
        0 -> transformIntType(roundToDecimal(source, targetScale).toBigIntegerExact())
        // target source is less than 0, we round this to integer
        in -Int.MAX_VALUE..-1 -> transformIntType(roundToDecimal(source, targetScale).toBigIntegerExact())
        // target source is bigger than 0, we round this to big decimal
        else -> roundToDecimal(source, targetScale)
    }

    // round decimal number to given target scale
    private fun roundToDecimal(source: Number, targetScale: Int): BigDecimal {
        return if (source >= 0.0) {
            roundHelper(source, targetScale)
        } else {
            -(roundHelper(-source, targetScale)) as BigDecimal
        }
    }

    private fun roundHelper(positiveSource: Number, targetScale: Int) = bigDecimalOf(positiveSource).multiply(
        (
            BigDecimal.ONE
                .scaleByPowerOfTen(targetScale)
            )
    ).add(bigDecimalOf(0.5)).setScale(0, RoundingMode.FLOOR).multiply(
        BigDecimal.ONE.scaleByPowerOfTen(-targetScale)
    )

    // During the implementation of the round function, I figured that the unaryMinus could potentially introduce some edge cases.
    operator fun Number.unaryMinus(): Number {
        return when (this) {
            // For some reasons most of the extended operator for numbers do not have int
            // if approved, could add the int case to those operator functions.
            is Int -> -this
            // this is an unfortunate edge case
            // taking bytes for example, bytes has 8 bits, signed byte ranges from -128 to 127, using two's complement
            // that is -128 ->   10000000
            // to negate it, we flip all the bits then +1
            // filp them we have 01111111
            // add one(place value) we have   10000000
            // which is again -128
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
}
