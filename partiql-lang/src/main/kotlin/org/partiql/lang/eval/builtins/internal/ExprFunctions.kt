package org.partiql.lang.eval.builtins.internal

import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.errIntOverflow
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.util.exprValue
import org.partiql.types.StaticType
import java.math.BigInteger

/**
 * Prototype of `(Number) -> Number` as a PartiQL ExprFunction.
 */
internal abstract class ExprFunctionUnaryNumeric(name: String) : ExprFunction {

    abstract fun call(x: Number): Number

    override val signature = FunctionSignature(
        name = name,
        requiredParameters = listOf(StaticType.NUMERIC),
        returnType = StaticType.NUMERIC,
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val x = required[0].numberValue()
        val result = call(x)
        return result.exprValue()
    }
}

/**
 * Prototype of `(Number, Number) -> Number` as a PartiQL ExprFunction.
 */
internal abstract class ExprFunctionBinaryNumeric(name: String) : ExprFunction {

    abstract fun call(x: Number, y: Number): Number

    override val signature = FunctionSignature(
        name = name,
        requiredParameters = listOf(StaticType.NUMERIC, StaticType.NUMERIC),
        returnType = StaticType.NUMERIC,
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val x = required[0].numberValue()
        val y = required[1].numberValue()
        val result = call(x, y)
        return result.exprValue()
    }
}

/**
 * Prototype of `(v: T) -> Int` where the action applies some measure to v
 */
internal abstract class ExprFunctionMeasure(name: String, type: StaticType) : ExprFunction {

    companion object {

        /**
         * Placed here rather than StaticType as an internal helper rather than an extension of StaticType
         */
        @JvmField
        val BITSTRING = StaticType.unionOf(StaticType.SYMBOL, StaticType.STRING, StaticType.BLOB, StaticType.CLOB)
    }

    abstract fun call(value: ExprValue): Int

    override val signature = FunctionSignature(
        name = name,
        requiredParameters = listOf(type),
        returnType = StaticType.INT,
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val value = required[0]
        val units = call(value)
        return ExprValue.newInt(units)
    }
}

// wrapper for transform function result to corresponding integer type
internal fun transformIntType(n: BigInteger): Number = when (n) {
    in Int.MIN_VALUE.toBigInteger()..Int.MAX_VALUE.toBigInteger() -> n.toInt()
    in Long.MIN_VALUE.toBigInteger()..Long.MAX_VALUE.toBigInteger() -> n.toLong()
    /**
     * currently PariQL-lang-kotlin did not support integer value bigger than 64 bits.
     */
    else -> errIntOverflow(8)
}
