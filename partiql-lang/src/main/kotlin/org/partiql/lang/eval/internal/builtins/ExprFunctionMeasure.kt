package org.partiql.lang.eval.internal.builtins

import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

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
