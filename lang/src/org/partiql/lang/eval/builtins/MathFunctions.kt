package org.partiql.lang.eval.builtins

import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A place to keep supported mathematical functions. We are missing many in comparison to PostgresQL.
 * https://www.postgresql.org/docs/9.5/functions-math.html
 */
object MathFunctions {

    fun create(valueFactory: ExprValueFactory): List<ExprFunction> = listOf(
        UnaryDoubleToInt("ceil", valueFactory) { ceil(it).toInt() },
        UnaryDoubleToInt("ceiling", valueFactory) { ceil(it).toInt() },
        UnaryDoubleToInt("floor", valueFactory) { floor(it).toInt() },
    )
}

private val numericTypes =
    setOf(StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.INT, StaticType.FLOAT, StaticType.DECIMAL)

/**
 * A convenience class to wrap `(Double) -> Int` as a PartiQL ExprFunction
 *
 * @property identifier Symbol for the given function
 * @property valueFactory
 * @property function Function to invoke for the given signature
 * @constructor
 */
private class UnaryDoubleToInt(
    private val identifier: String,
    private val valueFactory: ExprValueFactory,
    private val function: (Double) -> Int,
) : ExprFunction {

    override val signature = FunctionSignature(
        identifier,
        listOf(AnyOfType(numericTypes)),
        returnType = StaticType.INT,
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val v = required.first().numberValue()
        return valueFactory.newInt(function.invoke(v.toDouble()))
    }
}
