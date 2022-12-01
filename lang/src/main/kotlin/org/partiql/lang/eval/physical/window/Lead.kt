package org.partiql.lang.eval.physical.window

import org.partiql.annotations.PartiQLExperimental
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.operators.ValueExpression

// TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
// TODO: Decide if we should reduce the code duplication by combining lead and lag function.
@PartiQLExperimental
internal class Lead : NavigationWindowFunction() {

    override val name = "lead"

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
