package org.partiql.lang.ots_work.plugins.standard.functions

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.errIntOverflow
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.numberValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.operators.ALL_NUMBER_TYPES
import org.partiql.lang.ots_work.plugins.standard.valueFactory
import org.partiql.lang.util.bigDecimalOf
import java.lang.Double.NEGATIVE_INFINITY
import java.lang.Double.NaN
import java.lang.Double.POSITIVE_INFINITY
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun createNumericFunctionSignature(funcName: String) =
    FunctionSignature(
        funcName,
        listOf(ALL_NUMBER_TYPES),
        returnType = ALL_NUMBER_TYPES,
    )

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

object Ceil : ScalarFunction {
    override val signature: FunctionSignature =
        createNumericFunctionSignature("ceil")

    override fun callWithRequired(required: List<ExprValue>): ExprValue =
        when (val sourceNumber = required.first().numberValue()) {
            POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN -> sourceNumber
            // support for numbers that are larger than 64 bits.
            else -> transformIntType(bigDecimalOf(sourceNumber).setScale(0, RoundingMode.CEILING).toBigIntegerExact())
        }.exprValue(valueFactory)
}

object Ceiling : ScalarFunction {
    override val signature: FunctionSignature =
        createNumericFunctionSignature("ceiling")

    override fun callWithRequired(required: List<ExprValue>): ExprValue =
        when (val sourceNumber = required.first().numberValue()) {
            POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN -> sourceNumber
            // support for numbers that are larger than 64 bits.
            else -> transformIntType(bigDecimalOf(sourceNumber).setScale(0, RoundingMode.CEILING).toBigIntegerExact())
        }.exprValue(valueFactory)
}

object Floor : ScalarFunction {
    override val signature: FunctionSignature =
        createNumericFunctionSignature("floor")

    override fun callWithRequired(required: List<ExprValue>): ExprValue =
        when (val sourceNumber = required.first().numberValue()) {
            POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN -> sourceNumber
            else -> transformIntType(bigDecimalOf(sourceNumber).setScale(0, RoundingMode.FLOOR).toBigIntegerExact())
        }.exprValue(valueFactory)
}
