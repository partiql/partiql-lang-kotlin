package org.partiql.lang.eval.physical

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.util.toIntExact

/**
 * Sets the value of a variable which is stored in an [EvaluatorState].
 *
 * To obtain an instance of [SetVariableFunc] that sets a specific variable, see [toSetVariableFunc]. This is because
 * [EvaluatorState.registers] contains the current value of all variables in an SFW query.  As such, allowing direct
 * access to the registers is very risky--for example, if the wrong register is set the query will have the incorrect
 * result or trigger an evaluation-time exception.
 *
 * To mitigate this risk, [EvaluatorState.registers] is marked to `internal` to avoid exposing it publicly and
 * [SetVariableFunc] is provided to allow custom operator implementation to assign values to only specific variables
 * when needed.
 *
 * Parameters:
 *
 * - [EvaluatorState] - The object containing the current state of the evaluator and current values of the variables.
 * - [ExprValue] - The value to set.
 */
typealias SetVariableFunc = (EvaluatorState, ExprValue) -> Unit

/**
 * Creates a [SetVariableFunc] tied to the variable identified in the [PartiqlPhysical.VarDecl] receiver object that
 * can be used to set the value of the variable at evaluation time.  This method is considerably safer than direct
 * access to the [EvaluatorState.registers] array, which is marked as `internal` to reduce the likelihood of being
 * clobbered by a physical operator implementation which was not supplied by this library.
 */
internal fun PartiqlPhysical.VarDecl.toSetVariableFunc(): SetVariableFunc {
    val index = this.index.value.toIntExact()
    return { state, value -> state.registers[index] = value }
}

/**
 * Transfers all [ExprValue]s in [source] to the [target] [EvaluatorState]'s registers
 */
internal fun transferState(target: EvaluatorState, source: Array<ExprValue>) {
    if (target.registers.size != source.size) {
        throw EvaluationException("No", ErrorCode.EVALUATOR_GENERIC_EXCEPTION, null, null, true)
    }
    target.registers.forEachIndexed { index, _ -> target.registers[index] = source[index] }
}
