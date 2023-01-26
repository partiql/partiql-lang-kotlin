package org.partiql.lang.eval.physical.window

import org.partiql.annotations.ExperimentalWindowFunctions
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.operators.ValueExpression
import org.partiql.lang.eval.physical.toSetVariableFunc

/**
 * This abstract class holds some common logic for navigation window function, i.e., LAG, LEAD
 * TODO: When we support FIRST_VALUE, etc, we probably need to modify the process row function, since those function requires frame
 */
@ExperimentalWindowFunctions
abstract class NavigationWindowFunction() : WindowFunction {

    lateinit var currentPartition: List<Array<ExprValue>>
    private var currentPos: Int = 0

    override fun reset(partition: List<Array<ExprValue>>) {
        currentPartition = partition
        currentPos = 0
    }

    override fun processRow(
        state: EvaluatorState,
        arguments: List<ValueExpression>,
        windowVarDecl: PartiqlPhysical.VarDecl
    ) {
        state.load(currentPartition[currentPos])
        val value = processRow(state, arguments, currentPos)
        // before we declare the window function result, we need to go back to the current row
        state.load(currentPartition[currentPos])
        windowVarDecl.toSetVariableFunc().invoke(state, value)
        // make sure the change of state is reflected in the partition
        // so the result of the current window function won't get removed by the time we process the next window function at the same row level.
        currentPartition[currentPos][windowVarDecl.index.value.toInt()] = value
        currentPos += 1
    }

    abstract fun processRow(state: EvaluatorState, arguments: List<ValueExpression>, currentPos: Int): ExprValue
}
