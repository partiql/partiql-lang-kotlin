package org.partiql.lang.eval.builtins

import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import java.lang.Double.NEGATIVE_INFINITY
import java.lang.Double.NaN
import java.lang.Double.POSITIVE_INFINITY

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
 * A convenience class to wrap `(Double) -> Int` as a PartiQL ExprFunction
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

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val v = required.first().numberValue()
        return when (val result = function.invoke(v)) {
            POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN -> valueFactory.newFloat(result.toDouble())
            else -> valueFactory.newInt(result.toInt())
        }
    }
}

private fun ceil(n: Number): Number = when (n) {
    POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN -> n
    else -> kotlin.math.ceil(n.toDouble())
}

private fun floor(n: Number): Number = when (n) {
    POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN -> n
    else -> kotlin.math.floor(n.toDouble())
}
