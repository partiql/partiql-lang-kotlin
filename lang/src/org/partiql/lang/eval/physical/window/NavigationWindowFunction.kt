package org.partiql.lang.eval.physical.window

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.operators.ValueExpression
import org.partiql.lang.eval.physical.toSetVariableFunc

/**
 * This abstract class holds some common logic for navigation window function, i.e., LAG, LEAD
 *
 * TODO: When we support FIRST_VALUE, etc, we need to modify the process row function, since those function requires frame
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
        windowVarDecl: PartiqlPhysical.VarDecl
    ) {
        val value = processRow(state, arguments, currentPos)
        windowVarDecl.toSetVariableFunc()(state, value)
        currentPos += 1
    }

    abstract fun processRow(state: EvaluatorState, arguments: List<ValueExpression>, currentPos: Int): ExprValue
}
