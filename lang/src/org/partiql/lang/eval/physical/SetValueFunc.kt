package org.partiql.lang.eval.physical

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.util.toIntExact

/**
 * Sets the value of a variable which is stored in an [EvaluatorState].
 *
 * To obtain an instance of [SetVariableFunc] that sets a specific variable, see [toSetVariableFunc], which is
 * intentionlly `internal` to avoid exposing it to customers.  This is because [EvaluatorState.registers] contains the
 * current value of all variables in an SFW query.  As such, allowing direct access to the registers is very risky--for
 * example, if the wrong register is set the query will have the incorrect result or trigger an evaluation-time
 * exception.
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
internal fun PartiqlPhysical.VarDecl.toSetVariableFunc(): SetVariableFunc =
    { state, value -> state.registers[this.index.value.toIntExact()] = value }
