package org.partiql.lang.eval.physical

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.util.toIntExact

/**
 * Sets the value of a variable which is stored in an [EvaluatorState].
 *
 * To obtain an instance of [SetVariableFunc] that sets a specific variable, see [toSetVariableFunc].
 *
 * [EvaluatorState.registers] contains the current value of all variables in an SFW query.  As such, allowing
 * direct access to the registers is very risky--for example, if the wrong register is set the query will have
 * the incorrect result or trigger an evaluation-time exception.
 *
 * To mitigate this risk, [EvaluatorState.registers] is set to `internal` to avoid exposing it publicly and
 * [SetVariableFunc] is provided.
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
 * clobbered by custom operator implementations.
 *
 * Of course, it is still possible to circumvent this safety by constructing [PartiqlPhysical.VarDecl] with an
 * arbitrary index, but doing this in a way that avoids introducing evaluation-time bugs could be challenging.  In
 * general, it is safest to not manipulate register indexes assigned to [PartiqlPhysical.VarDecl] by the query planner
 * (the [org.partiql.lang.planner.transforms.LogicalToLogicalResolvedVisitorTransform] pass specifically).  If these
 * index values must be manipulated, it must be done with extreme care and thorough testing.
 */
fun PartiqlPhysical.VarDecl.toSetVariableFunc(): SetVariableFunc =
    { state, value -> state.registers[this.index.value.toIntExact()] = value }
