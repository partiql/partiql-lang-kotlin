package org.partiql.lang.eval.physical.window

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.operators.ValueExpression

class Lead : NavigationWindowFunction() {

    override val signature: WindowFunctionSignature = WindowFunctionSignature(
        name = "lead",
    )

    override fun processRow(state: EvaluatorState, arguments: List<ValueExpression>, currentPos: Int): ExprValue {
        val (target, offset, default) = when (arguments.size) {
            1 -> listOf(arguments[0], null, null)

            2 -> listOf(arguments[0], arguments[1], null)

            3 -> listOf(arguments[0], arguments[1], arguments[2])

            else -> error("Wrong number of Parameter for Lag Function")
        }

        val offsetValue = offset?.let {
            val numberValue = it.invoke(state).numberValue().toLong()
            if (numberValue >= 0) {
                numberValue
            } else {
                error("offset need to be non-negative integer")
            }
        } ?: 1L // default offset is one
        val defaultValue = default?.invoke(state) ?: state.valueFactory.nullValue
        val targetIndex = currentPos + offsetValue

        if (targetIndex <= currentPartition.lastIndex) {
            val targetRow = currentPartition[targetIndex.toInt()]
            state.load(targetRow)
            return target!!.invoke(state)
        } else {
            return defaultValue
        }
    }
}
