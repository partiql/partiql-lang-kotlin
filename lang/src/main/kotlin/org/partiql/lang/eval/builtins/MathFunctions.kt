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
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.div
import org.partiql.lang.util.plus
import org.partiql.lang.util.times
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
