package org.partiql.lang.eval.internal.builtins

import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.internal.ext.exprValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

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