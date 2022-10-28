package org.partiql.lang.eval.physical.window

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.operators.ValueExpression

/**
 * This abstract class holds some common logic for navigation window function, i.e., LAG, LEAD
 *
 * TODO: When we support FIRST_VALUE, etc, we probably need to modify the process row function, since those function requires frame
 */
abstract class NavigationWindowFunction() : WindowFunction {

    lateinit var currentPartition: List<Array<ExprValue>>
    var currentPos: Int = 0

    override fun reset(partition: List<Array<ExprValue>>) {
        currentPartition = partition
        currentPos = 0
    }

    override fun processRow(
        state: EvaluatorState,
        arguments: List<ValueExpression>,
        windowVarDecl: SetVariableFunc
    ) {
        state.load(currentPartition[currentPos])
        val value = processRow(state, arguments, currentPos)
        // before we declare the window function result, we need to go back to the current row
        state.load(currentPartition[currentPos])
        windowVarDecl(state, value)
        currentPos += 1
    }

    abstract fun processRow(state: EvaluatorState, arguments: List<ValueExpression>, currentPos: Int): ExprValue
}
