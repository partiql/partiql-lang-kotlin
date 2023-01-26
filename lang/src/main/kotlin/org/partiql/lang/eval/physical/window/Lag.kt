package org.partiql.lang.eval.physical.window

import org.partiql.annotations.ExperimentalWindowFunctions
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.operators.ValueExpression

// TODO: Decide if we should reduce the code duplication by combining lead and lag function
@ExperimentalWindowFunctions
internal class Lag : NavigationWindowFunction() {
    override val name = "lag"

    companion object {
        const val DEFAULT_OFFSET_VALUE = 1L
    }

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
        } ?: DEFAULT_OFFSET_VALUE
        val defaultValue = default?.invoke(state) ?: ExprValue.nullValue
        val targetIndex = currentPos - offsetValue

        if (targetIndex >= 0 && targetIndex <= currentPartition.lastIndex) {
            val targetRow = currentPartition[targetIndex.toInt()]
            state.load(targetRow)
            return target!!.invoke(state)
        } else {
            return defaultValue
        }
    }
}
